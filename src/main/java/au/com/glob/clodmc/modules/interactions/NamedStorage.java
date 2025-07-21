package au.com.glob.clodmc.modules.interactions;

import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Players;
import au.com.glob.clodmc.util.StringUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jspecify.annotations.NullMarked;

/** If a container has been named in an anvil, show that name when looking at it */
@NullMarked
public class NamedStorage implements Module, Listener {
  @EventHandler
  public void onPlayerMove(PlayerMoveEvent event) {
    Player player = event.getPlayer();
    Block block = player.getTargetBlockExact(Players.INTERACTION_RANGE);
    if (block == null) {
      return;
    }
    BlockState blockState = block.getState();

    if (blockState instanceof Container container) {
      Component name = container.customName();
      if (name != null) {
        player.sendActionBar(StringUtil.asComponent("<yellow>" + StringUtil.asText(name)));
      }
    }
  }
}
