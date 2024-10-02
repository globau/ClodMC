package au.com.glob.clodmc.modules.interactions;

import au.com.glob.clodmc.modules.Module;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * Allow waxing an item-frame to prevent item rotation/removal and allow click-through to containers
 */
public class WaxedItemFrames implements Module, Listener {
  private static final @NotNull NamespacedKey WAXED_KEY = new NamespacedKey("clod-mc", "waxed");

  @EventHandler
  public void onPlayerInteractEntity(@NotNull PlayerInteractEntityEvent event) {
    if (!(event.getRightClicked() instanceof ItemFrame itemFrame)) {
      return;
    }

    PersistentDataContainer persistentData = itemFrame.getPersistentDataContainer();
    Player player = event.getPlayer();

    // waxing
    EquipmentSlot hand = event.getHand();
    ItemStack itemInHand = player.getInventory().getItem(hand);
    if (itemInHand.getType() == Material.HONEYCOMB) {
      // not already waxed
      if (persistentData.has(WAXED_KEY)) {
        event.setCancelled(true);
        return;
      }

      // set waxed attribute and consume honeycomb
      persistentData.set(WAXED_KEY, PersistentDataType.BYTE, (byte) 1);
      if (player.getGameMode() == GameMode.SURVIVAL) {
        player.getInventory().getItem(event.getHand()).setAmount(itemInHand.getAmount() - 1);
      }

      // sound and particles
      Location loc = itemFrame.getLocation();
      player.playSound(loc, Sound.ITEM_HONEYCOMB_WAX_ON, 1.0f, 1.0f);

      World world = player.getWorld();
      BlockFace facing = itemFrame.getFacing();
      Vector right;
      Vector up;
      if (facing == BlockFace.UP || facing == BlockFace.DOWN) {
        right = new Vector(1, 0, 0);
        up = new Vector(0, 0, 1);
      } else {
        Vector direction = facing.getDirection();
        right = new Vector(direction.getZ(), 0, -direction.getX());
        up = new Vector(0, 1, 0);
      }
      for (int i = 0; i < 7; i++) {
        world.spawnParticle(
            Particle.WAX_ON,
            loc.clone()
                .add(right.clone().multiply(Math.random() - 0.5))
                .add(up.clone().multiply(Math.random() - 0.5)),
            1);
      }

      event.setCancelled(true);
      return;
    }

    // interacting with waxed item-frame (while not sneaking)
    if (!player.isSneaking() && persistentData.has(WAXED_KEY)) {
      // prevent rotation or popping out item
      event.setCancelled(true);

      // open container
      Block attachedBlock =
          itemFrame.getLocation().getBlock().getRelative(itemFrame.getAttachedFace());
      if (attachedBlock.getState() instanceof Container container) {
        player.openInventory(container.getInventory());
      }
    }
  }
}
