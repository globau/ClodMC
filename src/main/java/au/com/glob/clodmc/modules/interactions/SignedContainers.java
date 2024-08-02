package au.com.glob.clodmc.modules.interactions;

import au.com.glob.clodmc.modules.Module;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

/** When right-clicking on a waxed sign attached to a chest, open the chest instead of noop */
public class SignedContainers implements Module, Listener {
  @EventHandler
  public void onPlayerInteract(@NotNull PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
      return;
    }

    Block clickedBlock = event.getClickedBlock();
    if (clickedBlock == null) {
      return;
    }

    BlockState blockState = clickedBlock.getState();
    if (!(blockState instanceof Sign sign) || !sign.isWaxed()) {
      return;
    }

    BlockData blockData = clickedBlock.getBlockData();
    if (blockData instanceof WallSign wallSign) {
      Block attachedBlock = clickedBlock.getRelative(wallSign.getFacing().getOppositeFace());
      if (attachedBlock.getState() instanceof Container container) {
        event.setCancelled(true);
        event.getPlayer().openInventory(container.getInventory());
      }
    }
  }
}
