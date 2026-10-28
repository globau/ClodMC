package au.com.glob.clodmc.datafile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/**
 * access to player data files indexed by uuid.
 *
 * <p>files are always loaded fresh from disk. use read() for a snapshot that cannot be saved, and
 * update() to load, modify, and save a file in one step.
 */
@NullMarked
public final class PlayerDataFiles {
  private PlayerDataFiles() {}

  private static final Map<UUID, PlayerDataFile> updating = new HashMap<>();

  // load a read-only snapshot of a player's data file
  public static PlayerDataFile read(final Player player) {
    return read(player.getUniqueId());
  }

  // load a read-only snapshot of a player's data file
  public static PlayerDataFile read(final UUID uuid) {
    return new PlayerDataFile(uuid);
  }

  // load, modify, and save a player's data file
  public static void update(final Player player, final Consumer<PlayerDataFile> change) {
    update(player.getUniqueId(), change);
  }

  // load, modify, and save a player's data file; nested updates share the outer instance
  public static void update(final UUID uuid, final Consumer<PlayerDataFile> change) {
    if (!Bukkit.isPrimaryThread()) {
      throw new IllegalStateException("player data files must be updated on the main thread");
    }

    final PlayerDataFile active = updating.get(uuid);
    if (active != null) {
      change.accept(active);
      return;
    }

    final PlayerDataFile dataFile = new PlayerDataFile(uuid);
    updating.put(uuid, dataFile);
    try {
      change.accept(dataFile);
      dataFile.save();
    } finally {
      updating.remove(uuid);
    }
  }
}
