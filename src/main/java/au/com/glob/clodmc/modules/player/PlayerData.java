package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.Module;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerData implements Module, Listener {
  private static PlayerData instance;
  private final File playerConfigPath;
  private final Map<UUID, Config> onlinePlayers = new HashMap<>();

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

  @EventHandler
  public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
    Player player = event.getPlayer();
    Config config = Config.of(player.getUniqueId());
    config.set("player.name", player.getName());
    config.set(
        "player.last_login",
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    config.save();

    this.onlinePlayers.put(player.getUniqueId(), config);
  }

  @EventHandler
  public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
    this.onlinePlayers.remove(event.getPlayer().getUniqueId());
  }

  public static class Config extends YamlConfiguration {
    private File file;

    public static @NotNull Config of(UUID uuid) {
      File file = new File(instance.playerConfigPath, uuid + ".yml");
      Config config = new Config();
      try {
        config.load(file);
      } catch (FileNotFoundException e) {
        // ignore
      } catch (IOException | InvalidConfigurationException e) {
        ClodMC.logWarning("loading " + file + ": " + e);
      }
      config.file = file;
      return config;
    }

    public void save() {
      try {
        this.save(this.file);
      } catch (IOException e) {
        ClodMC.logError(instance.playerConfigPath + ": save failed: " + e);
      }
    }
  }
}
