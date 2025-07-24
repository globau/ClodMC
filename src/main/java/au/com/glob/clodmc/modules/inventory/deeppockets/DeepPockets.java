package au.com.glob.clodmc.modules.inventory.deeppockets;

import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Players;
import io.papermc.paper.event.player.PlayerPickItemEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.jspecify.annotations.NullMarked;

/** When picking blocks, look inside held shulker boxes too */
@NullMarked
public class DeepPockets implements Module, Listener {

  @EventHandler
  public void onPlayerPickItem(PlayerPickItemEvent event) {
    Player player = event.getPlayer();

    // nothing to do if the item is readily available, or the player isn't in survival mode
    if (event.getSourceSlot() != -1 || !player.getGameMode().equals(GameMode.SURVIVAL)) {
      return;
    }

    PlayerInventory playerInventory = player.getInventory();

    // find block player is targeting
    Block target = player.getTargetBlockExact(Players.INTERACTION_RANGE);
    if (target == null) {
      return;
    }

    // find slots with shulkers of any colour
    List<Integer> playerSlots = new ArrayList<>();
    for (Material shulkerMaterial : Tag.SHULKER_BOXES.getValues()) {
      HashMap<Integer, ? extends ItemStack> shulkers = playerInventory.all(shulkerMaterial);
      playerSlots.addAll(shulkers.keySet());
    }
    Collections.sort(playerSlots);

    // search for shulkers holding the target item
    List<ShulkerItemStack> shulkerItemStacks = new ArrayList<>();
    for (int playerSlot : playerSlots) {
      // sanity check and cast
      ItemStack playerItemStack = playerInventory.getItem(playerSlot);
      if (playerItemStack == null
          || !(playerItemStack.getItemMeta() instanceof BlockStateMeta playerBlockStateMeta)
          || !(playerBlockStateMeta.getBlockState() instanceof ShulkerBox shulker)) {
        continue;
      }

      // find matching items inside shulker
      Inventory shulkerInventory = shulker.getInventory();
      shulkerInventory.all(target.getType()).entrySet().stream()
          .map(
              (Map.Entry<Integer, ? extends ItemStack> entry) ->
                  new ShulkerItemStack(playerSlot, entry.getKey(), entry.getValue().getAmount()))
          .forEach(shulkerItemStacks::add);
    }

    // no matches
    if (shulkerItemStacks.isEmpty()) {
      this.playFailureSound(player);
      return;
    }

    // find the largest stack across all shulkers
    ShulkerItemStack largestShulkerStack =
        shulkerItemStacks.stream()
            .max(Comparator.comparingInt((ShulkerItemStack itemStack) -> itemStack.amount))
            .orElse(null);
    int playerSlot = largestShulkerStack.playerSlot;
    int shulkerSlot = largestShulkerStack.shulkerSlot;

    // grab stacks
    ItemStack playerItemStack = playerInventory.getItem(playerSlot);
    BlockStateMeta playerBlockStateMeta =
        (BlockStateMeta) Objects.requireNonNull(playerItemStack).getItemMeta();
    ShulkerBox shulker = (ShulkerBox) playerBlockStateMeta.getBlockState();
    ItemStack shulkerItemStack = shulker.getInventory().getItem(shulkerSlot);

    // get itemstack from target slot
    int targetSlot = event.getTargetSlot();
    ItemStack targetItemStack = playerInventory.getItem(targetSlot);

    // if the shulker is in the target slot, we need to move the shulker
    // to an empty slot
    if (playerSlot == targetSlot) {
      int emptySlot = playerInventory.firstEmpty();

      // full inventory; failure
      if (emptySlot == -1) {
        this.playFailureSound(player);
        return;
      }

      // move shulker into empty slot
      playerInventory.setItem(emptySlot, targetItemStack);
      playerInventory.clear(targetSlot);
      playerSlot = emptySlot;
      targetItemStack = null;
    }

    // replace target slot with stack from inside the shulker
    playerInventory.setItem(targetSlot, shulkerItemStack);

    // place the now-replaced target stack into the shulker
    if (targetItemStack == null) {
      shulker.getInventory().clear(shulkerSlot);
    } else {
      shulker.getInventory().setItem(shulkerSlot, targetItemStack);
    }
    playerBlockStateMeta.setBlockState(shulker);
    playerItemStack.setItemMeta(playerBlockStateMeta);
    playerInventory.setItem(playerSlot, playerItemStack);

    // select the target in the hotbar
    playerInventory.setHeldItemSlot(targetSlot);

    // cancel the event as we've handled it
    event.setCancelled(true);
  }

  private void playFailureSound(Player player) {
    player.playSound(player, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 0.5f, 0.5f);
  }
}
