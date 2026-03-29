package au.com.glob.clodmc.modules.server.heapmap;

import au.com.glob.clodmc.annotations.Audience;
import au.com.glob.clodmc.annotations.Doc;
import au.com.glob.clodmc.events.AfkStateChangeEvent;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Schedule;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jspecify.annotations.NullMarked;

@Doc(
    audience = Audience.SERVER,
    title = "Heat Map",
    description = "Track minutes a chunk is occupied by at least one player")
@NullMarked
public class HeatMap extends Module implements Listener {
  private final HashSet<UUID> afkPlayers = new HashSet<>();

  public HeatMap() {
    Schedule.periodically(
        20 * 60,
        20 * 60,
        () -> {
          final Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
          if (onlinePlayers.isEmpty()) {
            return;
          }

          final HashSet<Chunk> inhabitedChunks = new HashSet<>(onlinePlayers.size());
          for (final Player player : onlinePlayers) {
            if (!this.afkPlayers.contains(player.getUniqueId())) {
              inhabitedChunks.add(player.getChunk());
            }
          }

          Schedule.asynchronously(
              () -> {
                try (final DB db = new DB()) {
                  db.incChunks(inhabitedChunks);
                }
              });
        });
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onAfkStateChange(final AfkStateChangeEvent event) {
    if (event.isAway()) {
      this.afkPlayers.add(event.getPlayer().getUniqueId());
    } else {
      this.afkPlayers.remove(event.getPlayer().getUniqueId());
    }
  }
}
