package au.com.glob.clodmc.modules.interactions;

import au.com.glob.clodmc.annotations.Audience;
import au.com.glob.clodmc.annotations.Doc;
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
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NullMarked;

@Doc(
    audience = Audience.PLAYER,
    title = "Waxed Item Frames",
    description =
        "Allow waxing an item-frame to prevent item rotation/removal and allow chest click-through")
@NullMarked
public class WaxedItemFrames implements Module, Listener {
  private static final NamespacedKey WAXED_KEY = new NamespacedKey("clod-mc", "waxed");

  // handle waxing item frames and click-through to containers
  @SuppressWarnings("MissingCasesInEnumSwitch")
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onPlayerInteractEntity(final PlayerInteractEntityEvent event) {
    if (!(event.getRightClicked() instanceof final ItemFrame itemFrame)) {
      return;
    }

    final PersistentDataContainer persistentData = itemFrame.getPersistentDataContainer();
    final Player player = event.getPlayer();

    // waxing
    final EquipmentSlot hand = event.getHand();
    final ItemStack itemInHand = player.getInventory().getItem(hand);
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
      final Location loc = itemFrame.getLocation();
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
      final Block attachedBlock =
          itemFrame.getLocation().getBlock().getRelative(itemFrame.getAttachedFace());
      if (attachedBlock.getState() instanceof final Container container) {
        player.openInventory(container.getInventory());
      }
    }
  }
}
