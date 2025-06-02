package au.com.glob.clodmc.datafile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlayerDataFiles {
  protected static final @NotNull Map<String, PlayerDataFile> instances = new HashMap<>();

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

  public static void unload(@NotNull Player player) {
    instances.remove("players/" + player.getUniqueId() + ".yml");
  }
}
