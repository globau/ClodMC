package au.com.glob.clodmc.modules.interactions;

import au.com.glob.clodmc.modules.Module;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
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
import org.jetbrains.annotations.NotNull;

/**
 * Allow waxing an item-frame to prevent item rotation/removal and allow click-through to containers
 */
public class WaxedItemFrames implements Module, Listener {
  private static final @NotNull NamespacedKey WAXED_KEY = new NamespacedKey("clod-mc", "waxed");

  @SuppressWarnings("MissingCasesInEnumSwitch")
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

      // sound
      Location loc = itemFrame.getLocation();
      player.playSound(loc, Sound.ITEM_HONEYCOMB_WAX_ON, 1.0f, 1.0f);

      // particles
      double x = loc.getX();
      double y = loc.getY();
      double z = loc.getZ();
      double randomX = 0.25;
      double randomY = 0.25;
      double randomZ = 0.25;
      switch (itemFrame.getFacing()) {
        case UP -> {
          randomY = 0;
          y += 0.1;
        }
        case DOWN -> {
          randomY = 0;
          y -= 0.1;
        }
        case NORTH -> {
          randomZ = 0;
          z -= 0.1;
        }
        case SOUTH -> {
          randomZ = 0;
          z += 0.1;
        }
        case EAST -> {
          randomX = 0;
          x += 0.1;
        }
        case WEST -> {
          randomX = 0;
          x -= 0.1;
        }
      }
      player.getWorld().spawnParticle(Particle.WAX_ON, x, y, z, 7, randomX, randomY, randomZ);

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
