package au.com.glob.clodmc.modules.interactions;

import au.com.glob.clodmc.modules.Module;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.ItemFrame;
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

  @EventHandler
  public void onPlayerInteract(@NotNull PlayerInteractEntityEvent event) {
    if (!(event.getRightClicked() instanceof ItemFrame itemFrame)) {
      return;
    }

    PersistentDataContainer persistentData = itemFrame.getPersistentDataContainer();

    // waxing
    EquipmentSlot hand = event.getHand();
    ItemStack itemInHand = event.getPlayer().getInventory().getItem(hand);
    if (itemInHand.getType() == Material.HONEYCOMB) {
      persistentData.set(WAXED_KEY, PersistentDataType.BYTE, (byte) 1);
      if (event.getPlayer().getGameMode() == GameMode.SURVIVAL) {
        event
            .getPlayer()
            .getInventory()
            .getItem(event.getHand())
            .setAmount(itemInHand.getAmount() - 1);
      }
      event.setCancelled(true);
      return;
    }

    // interacting with waxed item-frame (while not sneaking)
    if (!event.getPlayer().isSneaking()
        && persistentData.get(WAXED_KEY, PersistentDataType.BYTE) != null) {
      // prevent rotation or popping out item
      event.setCancelled(true);

      // open container
      Block attachedBlock =
          itemFrame.getLocation().getBlock().getRelative(itemFrame.getAttachedFace());
      if (attachedBlock.getState() instanceof Container container) {
        event.getPlayer().openInventory(container.getInventory());
      }
    }
  }
}
