package au.com.glob.clodmc.datafile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/** factory and cache for player data files indexed by uuid */
@NullMarked
public class PlayerDataFiles {
  protected static final Map<String, PlayerDataFile> instances = new HashMap<>();

  // get cached file or create new one
  public static PlayerDataFile of(String filename) {
    if (!instances.containsKey(filename)) {
      instances.put(filename, new PlayerDataFile(filename));
    }
    return instances.get(filename);
  }

  // get player data file by player object
  public static PlayerDataFile of(Player player) {
    return of(player.getUniqueId());
  }

  // get player data file by uuid
  public static PlayerDataFile of(UUID uuid) {
    return of("players/%s.yml".formatted(uuid));
  }

  // remove player from cache to free memory
  public static void unload(Player player) {
    instances.remove("players/%s.yml".formatted(player.getUniqueId()));
  }
}
