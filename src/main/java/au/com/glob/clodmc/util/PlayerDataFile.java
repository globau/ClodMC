package au.com.glob.clodmc.util;

import au.com.glob.clodmc.ClodMC;
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
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** player data file management */
public class PlayerDataFile extends YamlConfiguration {
  protected static final @NotNull Map<String, PlayerDataFile> instances = new HashMap<>();

  private static final @NotNull DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final @NotNull File file;
  private boolean exists;

  protected PlayerDataFile(@NotNull String filename) {
    super();

    if (!ConfigUtil.sanityChecked) {
      Bukkit.shutdown();
      throw new RuntimeException("config file loaded before sanity checks");
    }

    this.file = new File(ClodMC.instance.getDataFolder(), filename);
    try {
      this.load(this.file);
      this.exists = true;
    } catch (FileNotFoundException ignore) {
      // ignore
    } catch (IOException e) {
      Logger.error("failed to load " + this.file + ": " + e);
    } catch (InvalidConfigurationException e) {
      Logger.error("malformed " + this.file + ": " + e);
    }
  }

  public boolean fileExists() {
    return this.exists;
  }

  protected void save() {
    try {
      if (!this.file.getParentFile().exists() && !this.file.getParentFile().mkdirs()) {
        Logger.error(this.file + ": mkdir failed");
      }
      this.save(this.file);
    } catch (IOException e) {
      Logger.error(this.file + ": save failed: " + e);
    }
  }

  public @NotNull static PlayerDataFile of(@NotNull String filename) {
    if (!instances.containsKey(filename)) {
      instances.put(filename, new PlayerDataFile(filename));
    }
    return instances.get(filename);
  }

  public static @NotNull PlayerDataFile of(@NotNull Player player) {
    return of(player.getUniqueId());
  }

  public static @NotNull PlayerDataFile of(@NotNull UUID uuid) {
    return of("players/" + uuid + ".yml");
  }

  public static @NotNull List<UUID> knownUUIDs() {
    File playersPath = ClodMC.instance.getDataFolder().toPath().resolve("players").toFile();
    File[] ymlFiles = playersPath.listFiles((File file) -> file.getName().endsWith(".yml"));
    return ymlFiles == null
        ? List.of()
        : Arrays.stream(ymlFiles)
            .map((File file) -> file.getName().substring(0, file.getName().indexOf(".")))
            .map(UUID::fromString)
            .toList();
  }

  public static void unload(@NotNull Player player) {
    instances.remove("players/" + player.getUniqueId() + ".yml");
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

  public long getPlaytimeMins() {
    return this.getLong("player.playtime_min", 0);
  }

  public void setDateTime(@NotNull String path, @NotNull LocalDateTime dateTime) {
    this.set(path, dateTime.format(DATE_TIME_FORMATTER));
  }

  @Nullable public LocalDateTime getDateTime(@NotNull String path) {
    try {
      return LocalDateTime.parse(this.getString(path, ""), DATE_TIME_FORMATTER);
    } catch (DateTimeParseException e) {
      return null;
    }
  }
}
