package au.com.glob.clodmc.modules.interactions;

import au.com.glob.clodmc.annotations.Audience;
import au.com.glob.clodmc.annotations.Doc;
import au.com.glob.clodmc.events.PlayerTargetBlockEvent;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.ActionBar;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jspecify.annotations.NullMarked;

@Doc(
    audience = Audience.PLAYER,
    title = "Named Storage",
    description = "If a container has been named in an anvil, show that name when looking at it")
@NullMarked
public class NamedStorage implements Module, Listener {
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerTargetBlock(final PlayerTargetBlockEvent event) {
    final Player player = event.getPlayer();
    final Block block = event.getTargetBlock();

    if (block == null) {
      return;
    }
    final BlockState blockState = block.getState();

    if (blockState instanceof final Container container) {
      final Component name = container.customName();
      if (name != null) {
        ActionBar.info(player, name);
      }
    }
  }
}
