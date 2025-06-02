package au.com.glob.clodmc.util;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.datafile.PlayerDataFile;
import au.com.glob.clodmc.datafile.PlayerDataFiles;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Players {
  private static volatile @NotNull Map<String, UUID> whitelisted = new HashMap<>();

  public static @NotNull Map<String, UUID> getWhitelisted() {
    return whitelisted;
  }

  public static void updateWhitelisted() {
    Schedule.asynchronously(
        () -> {
          File playersPath = ClodMC.instance.getDataFolder().toPath().resolve("players").toFile();
          File[] ymlFiles = playersPath.listFiles((File file) -> file.getName().endsWith(".yml"));
          List<UUID> uuids =
              ymlFiles == null
                  ? List.of()
                  : Arrays.stream(ymlFiles)
                      .map((File file) -> file.getName().substring(0, file.getName().indexOf(".")))
                      .map(UUID::fromString)
                      .toList();

          Map<String, UUID> updatedWhitelist = new HashMap<>();
          for (UUID uuid : uuids) {
            PlayerDataFile config = PlayerDataFiles.of(uuid);
            updatedWhitelist.put(config.getPlayerName(), uuid);
          }
          whitelisted = Collections.unmodifiableMap(updatedWhitelist);
        });
  }

  public static boolean isWhitelisted(@NotNull String name) {
    return whitelisted.keySet().stream().anyMatch((String p) -> p.equalsIgnoreCase(name));
  }

  public static @Nullable UUID getWhitelistedUUID(@NotNull String name) {
    return whitelisted.entrySet().stream()
        .filter((Map.Entry<String, UUID> entry) -> entry.getKey().equalsIgnoreCase(name))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElse(null);
  }
}
