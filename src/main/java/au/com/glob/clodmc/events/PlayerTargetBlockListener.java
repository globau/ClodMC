package au.com.glob.clodmc.events;

import au.com.glob.clodmc.util.Players;
import java.util.UUID;
import java.util.WeakHashMap;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jspecify.annotations.NullMarked;

/** listens for player movement and fires target block events when view direction changes */
@NullMarked
public class PlayerTargetBlockListener implements Listener {
  private final WeakHashMap<UUID, ViewDirection> lastView = new WeakHashMap<>();
  private static final float MIN_ROTATION_CHANGE = 3.0f;

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerMove(final PlayerMoveEvent event) {
    final Player player = event.getPlayer();
    final Location to = event.getTo();

    if (player.getGameMode().equals(GameMode.SPECTATOR)) {
      return;
    }

    // only trigger event if the player's pitch and/or yaw has changed significantly
    final ViewDirection currentDirection =
        new ViewDirection(to.getWorld(), to.getYaw(), to.getPitch());
    final ViewDirection previousDirection = this.lastView.get(player.getUniqueId());

    if (previousDirection == null
        || !previousDirection.world().equals(to.getWorld())
        || currentDirection.rotationTo(previousDirection) >= MIN_ROTATION_CHANGE) {
      this.lastView.put(player.getUniqueId(), currentDirection);

      Bukkit.getPluginManager()
          .callEvent(
              new PlayerTargetBlockEvent(
                  player, player.getTargetBlockExact(Players.INTERACTION_RANGE)));
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerQuit(final PlayerQuitEvent event) {
    this.lastView.remove(event.getPlayer().getUniqueId());
  }
}
