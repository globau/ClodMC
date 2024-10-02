package au.com.glob.clodmc.modules.interactions;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.Module;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import vendored.com.jeff_media.customblockdata.CustomBlockData;

/** Allow waxing a pressure plate to prevent it activating */
public class WaxedPressurePlates implements Module, Listener {
  private static final @NotNull NamespacedKey WAXED_KEY = new NamespacedKey("clod-mc", "waxed");

  @EventHandler
  public void onPlayerInteract(@NotNull PlayerInteractEvent event) {
    Block block = event.getClickedBlock();
    if (block == null) {
      return;
    }
    if (!Tag.PRESSURE_PLATES.isTagged(block.getType())) {
      return;
    }

    // pressure plate activated
    if (event.getAction() == Action.PHYSICAL) {
      // cancel interaction if waxed
      PersistentDataContainer customBlockData = new CustomBlockData(block, ClodMC.instance);
      if (customBlockData.has(WAXED_KEY)) {
        event.setCancelled(true);
        return;
      }
      return;
    }

    // right-clicked
    if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
      // holding honeycomb
      EquipmentSlot hand = event.getHand();
      if (hand == null) {
        return;
      }
      ItemStack itemInHand = event.getPlayer().getInventory().getItem(hand);
      if (itemInHand.getType() != Material.HONEYCOMB) {
        return;
      }
      // not already waxed
      PersistentDataContainer customBlockData = new CustomBlockData(block, ClodMC.instance);
      if (customBlockData.has(WAXED_KEY)) {
        return;
      }
      // set waxed attribute and consume honeycomb
      customBlockData.set(WAXED_KEY, PersistentDataType.BYTE, (byte) 1);
      if (event.getPlayer().getGameMode() == GameMode.SURVIVAL) {
        event
            .getPlayer()
            .getInventory()
            .getItem(event.getHand())
            .setAmount(itemInHand.getAmount() - 1);
      }
      event.setCancelled(true);
    }
  }
}
