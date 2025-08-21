package au.com.glob.clodmc.modules.interactions.namedstorage;

import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Players;
import au.com.glob.clodmc.util.StringUtil;
import java.util.UUID;
import java.util.WeakHashMap;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jspecify.annotations.NullMarked;

/** If a container has been named in an anvil, show that name when looking at it */
@NullMarked
public class NamedStorage implements Module, Listener {
  private final WeakHashMap<UUID, ViewDirection> lastView = new WeakHashMap<>();
  private static final float MIN_ROTATION_CHANGE = 3.0f;

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerMove(PlayerMoveEvent event) {
    Player player = event.getPlayer();
    Location to = event.getTo();

    // only raytrace if the player's pitch and/or yaw has changed significantly
    ViewDirection currentView = new ViewDirection(to.getYaw(), to.getPitch());
    ViewDirection lastView = this.lastView.get(player.getUniqueId());
    if (lastView != null && currentView.distanceTo(lastView) < MIN_ROTATION_CHANGE) {
      return;
    }
    this.lastView.put(player.getUniqueId(), currentView);

    Block block = player.getTargetBlockExact(Players.INTERACTION_RANGE);
    if (block == null) {
      return;
    }
    BlockState blockState = block.getState();

    if (blockState instanceof Container container) {
      Component name = container.customName();
      if (name != null) {
        player.sendActionBar(
            StringUtil.asComponent("<yellow>%s".formatted(StringUtil.asText(name))));
      }
    }
  }
}
