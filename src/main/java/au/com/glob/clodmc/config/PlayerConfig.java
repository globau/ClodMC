package au.com.glob.clodmc.config;

import au.com.glob.clodmc.ClodMC;
import java.io.File;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerConfig extends Config {
  @SuppressWarnings("NotNullFieldNotInitialized")
  public static @NotNull PlayerConfig instance;

  @NotNull public static PlayerConfig of(@NotNull String filename) {
    if (Config.instances.containsKey(filename)) {
      PlayerConfig config = (PlayerConfig) instances.get(filename);
      if (config.isUnmodified()) {
        return config;
      }
    }
    instances.put(filename, new PlayerConfig(filename));
    return (PlayerConfig) instances.get(filename);
  }

  public static @NotNull PlayerConfig of(@NotNull Player player) {
    return of(player.getUniqueId());
  }

  public static @NotNull PlayerConfig of(@NotNull UUID uuid) {
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

  //

  protected PlayerConfig(@NotNull String filename) {
    super(filename);
  }

  public static void unload(@NotNull Player player) {
    Config.unload("players/" + player.getUniqueId() + ".yml");
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
}
