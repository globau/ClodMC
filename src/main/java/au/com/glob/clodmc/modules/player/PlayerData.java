package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.Module;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Statistic;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerData implements Module, Listener {
  private static PlayerData instance;
  private final File playerConfigPath;
  private final Map<UUID, Config> onlinePlayers = new HashMap<>();
  private final DateTimeFormatter dateFormatter =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  public PlayerData() {
    instance = this;

    this.playerConfigPath = new File(ClodMC.instance.getDataFolder(), "players");
    if (!this.playerConfigPath.exists() && !this.playerConfigPath.mkdirs()) {
      ClodMC.logWarning(this.playerConfigPath + ": mkdir failed");
    }
  }

  public static @NotNull Config of(@NotNull UUID uuid) {
    if (instance.onlinePlayers.containsKey(uuid)) {
      return instance.onlinePlayers.get(uuid);
    }
    return Config.of(uuid);
  }

  public static @NotNull Config of(@NotNull Player player) {
    return of(player.getUniqueId());
  }

  public static @NotNull List<UUID> seenUUIDs() {
    File[] ymlFiles =
        instance.playerConfigPath.listFiles((File file) -> file.getName().endsWith(".yml"));
    return ymlFiles == null
        ? List.of()
        : Arrays.stream(ymlFiles)
            .map((File file) -> file.getName().substring(0, file.getName().indexOf(".")))
            .map(UUID::fromString)
            .toList();
  }

  @EventHandler
  public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
    Player player = event.getPlayer();
    Config config = Config.of(player.getUniqueId());
    config.set("player.name", player.getName());
    config.set("player.last_login", LocalDateTime.now().format(this.dateFormatter));
    config.save();

    this.onlinePlayers.put(player.getUniqueId(), config);
  }

  @EventHandler
  public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
    Player player = event.getPlayer();

    this.onlinePlayers.remove(player.getUniqueId());

    try (Update config = new Update(player)) {
      config.set("player.last_logout", LocalDateTime.now().format(this.dateFormatter));
      config.set(
          "player.playtime_min",
          Math.round(player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20.0 / 60.0));
    }
  }

  public static class Config extends YamlConfiguration {
    private File file;
    public boolean exists;

    public static @NotNull Config of(UUID uuid) {
      File file = new File(instance.playerConfigPath, uuid + ".yml");
      Config config = new Config();
      config.file = file;
      config.exists = false;
      try {
        config.load(file);
        config.exists = true;
      } catch (FileNotFoundException e) {
        // ignore
      } catch (IOException | InvalidConfigurationException e) {
        ClodMC.logWarning("loading " + file + ": " + e);
      }
      return config;
    }

    private void save() {
      try {
        this.save(this.file);
      } catch (IOException e) {
        ClodMC.logError(instance.playerConfigPath + ": save failed: " + e);
      }
    }

    public @NotNull String getPlayerName() {
      return this.getString("player.name", "");
    }

    public @Nullable LocalDateTime getLastLogin() {
      return this.getDateTime("player.last_login");
    }

    public @Nullable LocalDateTime getLastLogout() {
      return this.getDateTime("player.last_logout");
    }

    private @Nullable LocalDateTime getDateTime(@NotNull String path) {
      try {
        return LocalDateTime.parse(this.getString(path, ""), PlayerData.instance.dateFormatter);
      } catch (DateTimeParseException e) {
        return null;
      }
    }
  }

  public static class Update implements AutoCloseable {
    private final Config config;
    private boolean modified;

    public Update(@NotNull Player player) {
      this.config = PlayerData.of(player.getUniqueId());
    }

    public Update(@NotNull UUID uuid) {
      this.config = PlayerData.of(uuid);
    }

    @Override
    public void close() {
      if (this.modified) {
        this.config.save();
      }
    }

    public boolean exists() {
      return this.config.exists;
    }

    public void set(@NotNull String path, @Nullable Object value) {
      this.config.set(path, value);
      this.modified = true;
    }

    public @Nullable ConfigurationSection getConfigurationSection(@NotNull String path) {
      return this.config.getConfigurationSection(path);
    }

    public List<?> getList(@NotNull String path) {
      return this.config.getList(path);
    }
  }
}
