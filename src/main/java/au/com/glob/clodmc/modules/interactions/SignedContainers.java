package au.com.glob.clodmc.modules.interactions;

import au.com.glob.clodmc.modules.Module;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

/** When right-clicking on a waxed sign attached to a chest, open the chest instead of noop */
public class SignedContainers implements Module, Listener {
  @EventHandler
  public void onPlayerInteract(@NotNull PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
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
        Player player = event.getPlayer();

        // remove wax with an axe for parity with copper
        if (event.getHand() != null
            && Tag.ITEMS_AXES.isTagged(player.getInventory().getItem(event.getHand()).getType())) {

          sign.setWaxed(false);
          sign.update();

          Location loc = sign.getLocation();
          player.playSound(loc, Sound.ITEM_AXE_WAX_OFF, 1.0f, 1.0f);
          player
              .getWorld()
              .spawnParticle(Particle.WAX_OFF, loc.add(0.5, 0.1, 0.5), 7, 0.25, 0, 0.25);
          return;
        }

        // otherwise open container sign is attached to
        player.openInventory(container.getInventory());
      }
    }
  }
}
