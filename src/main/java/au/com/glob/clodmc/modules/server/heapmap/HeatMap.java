package au.com.glob.clodmc.modules.server.heapmap;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.player.afk.AFK;
import au.com.glob.clodmc.util.Schedule;
import java.util.Collection;
import java.util.HashSet;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Track minutes a chunk is occupied by at least one player */
@NullMarked
public class HeatMap implements Module, Listener {
  private @Nullable AFK afk;

  public HeatMap() {
    Schedule.periodically(
        20 * 60,
        20 * 60,
        () -> {
          assert this.afk != null;

          Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
          if (onlinePlayers.isEmpty()) {
            return;
          }

          HashSet<Chunk> inhabitedChunks = new HashSet<>(onlinePlayers.size());
          for (Player player : onlinePlayers) {
            if (!this.afk.isAway(player)) {
              inhabitedChunks.add(player.getChunk());
            }
          }

          Schedule.asynchronously(
              () -> {
                try (DB db = new DB()) {
                  db.incChunks(inhabitedChunks);
                }
              });
        });
  }

  @Override
  public void loadConfig() {
    this.afk = ClodMC.getModule(AFK.class);
  }
}
