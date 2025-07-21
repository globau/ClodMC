package au.com.glob.clodmc.datafile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class PlayerDataFiles {
  protected static final Map<String, PlayerDataFile> instances = new HashMap<>();

  public static PlayerDataFile of(String filename) {
    if (!instances.containsKey(filename)) {
      instances.put(filename, new PlayerDataFile(filename));
    }
    return instances.get(filename);
  }

  public static PlayerDataFile of(Player player) {
    return of(player.getUniqueId());
  }

  public static PlayerDataFile of(UUID uuid) {
    return of("players/" + uuid + ".yml");
  }

  public static void unload(Player player) {
    instances.remove("players/" + player.getUniqueId() + ".yml");
  }
}
