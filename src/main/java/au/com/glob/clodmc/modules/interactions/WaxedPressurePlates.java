package au.com.glob.clodmc.modules.interactions;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.Module;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NullMarked;
import vendored.com.jeff_media.customblockdata.CustomBlockData;

/** Allow waxing a pressure plate to prevent it activating */
@NullMarked
public class WaxedPressurePlates implements Module, Listener {
  private static final NamespacedKey WAXED_KEY = new NamespacedKey("clod-mc", "waxed");

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
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
      Player player = event.getPlayer();
      // holding honeycomb
      EquipmentSlot hand = event.getHand();
      if (hand == null) {
        return;
      }
      ItemStack itemInHand = player.getInventory().getItem(hand);
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
      if (player.getGameMode() == GameMode.SURVIVAL) {
        player.getInventory().getItem(event.getHand()).setAmount(itemInHand.getAmount() - 1);
      }

      // sound and particles
      Location loc = block.getLocation();
      player.playSound(loc, Sound.ITEM_HONEYCOMB_WAX_ON, 1.0f, 1.0f);
      player.getWorld().spawnParticle(Particle.WAX_ON, loc.add(0.5, 0.1, 0.5), 7, 0.25, 0, 0.25);

      event.setCancelled(true);
    }
  }
}
