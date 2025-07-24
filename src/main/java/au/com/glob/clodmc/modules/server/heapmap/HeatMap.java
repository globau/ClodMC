package au.com.glob.clodmc.modules.server.heapmap;

import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Schedule;
import java.util.Collection;
import java.util.HashSet;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jspecify.annotations.NullMarked;

/** Track minutes a chunk is occupied by at least one player */
@NullMarked
public class HeatMap implements Module, Listener {
  public HeatMap() {
    DB db = new DB();

    Schedule.periodically(
        20 * 60,
        () -> {
          Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
          if (onlinePlayers.isEmpty()) {
            return;
          }

          HashSet<Chunk> inhabitedChunks = new HashSet<>(onlinePlayers.size());
          for (Player player : onlinePlayers) {
            inhabitedChunks.add(player.getChunk());
          }

          for (Chunk chunk : inhabitedChunks) {
            db.incChunk(chunk);
          }
        });
  }
}
