package au.com.glob.clodmc.modules.inventory.inventorysort;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.player.OpAlerts;
import au.com.glob.clodmc.util.Logger;
import au.com.glob.clodmc.util.StringUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.entity.Donkey;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Sort containers by shift+right-clicking in the inventory screen */
@NullMarked
public class InventorySort implements Listener, Module {
  static final Map<String, Integer> materialOrder = new HashMap<>(1477);

  public InventorySort() {
    super();
  }

  @Override
  public void loadConfig() {
    List<String> alerts = new ArrayList<>(0);
    List<String> warnings = new ArrayList<>(0);
    List<String> allMaterials = Registry.MATERIAL.stream().map(Enum::name).toList();

    // read inventory_order.txt. format follows.  english name is ignored
    // # comment
    // english name  :MATERIAL
    List<String> orderedMaterials = new ArrayList<>();
    InputStream resourceStream = ClodMC.instance.getResource("inventory_order.txt");
    if (resourceStream == null) {
      throw new RuntimeException("failed to read");
    }
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
      String line;
      int lineNo = 0;
      while ((line = reader.readLine()) != null) {
        lineNo++;
        if (line.isBlank() || line.startsWith("#")) {
          continue;
        }
        String[] parts = line.split(":", -1);
        if (parts.length != 2) {
          alerts.add("inventory_order.txt: invalid line " + lineNo);
          continue;
        }
        orderedMaterials.add(parts[1]);
      }
    } catch (IOException e) {
      Logger.error("inventory_order.txt: " + e.getMessage());
      return;
    }

    // build material --> SortItem mapping
    int index = 0;
    for (String material : orderedMaterials) {
      // must exist; these are only warnings as invalid items are ignored
      if (!allMaterials.contains(material)) {
        warnings.add("inventory_order.txt: invalid: " + material);
        continue;
      }

      // no duplicates
      if (materialOrder.containsKey(material)) {
        alerts.add("inventory_order.txt: duplicate: " + material);
        continue;
      }

      materialOrder.put(material, index);
      index++;
    }

    for (String name : allMaterials) {
      if (!materialOrder.containsKey(name)) {
        Material material =
            Registry.MATERIAL.stream()
                .filter((Material m) -> m.name().equals(name))
                .findFirst()
                .orElseThrow();
        alerts.add("inventory_order.txt: missing: " + StringUtil.asText(material) + " :" + name);
      }
    }

    for (String alert : alerts) {
      Logger.error(alert);
      OpAlerts.addAlert(alert);
    }
    for (String warning : warnings) {
      Logger.warning(warning);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player
        && event.getClick() == ClickType.SHIFT_RIGHT
        && event.getSlotType() == InventoryType.SlotType.CONTAINER)) {
      return;
    }

    Inventory inventory = event.getClickedInventory();
    if (inventory == null) {
      return;
    }
    if (inventory.isEmpty()) {
      return;
    }
    event.setCancelled(true);

    // find container start/stop slots
    int minSlot = 0;
    int maxSlot = inventory.getSize() - 1;
    if (inventory.getType() == InventoryType.PLAYER) {
      minSlot = 9;
      maxSlot = 35;
    } else if (inventory.getHolder() instanceof Donkey) {
      minSlot = 1;
    }

    // merge similar itemstacks
    List<ItemStack> combinedStacks = new ArrayList<>(inventory.getSize());
    for (int slot = minSlot; slot <= maxSlot; slot++) {
      ItemStack invStack = inventory.getItem(slot);
      if (invStack == null) {
        continue;
      }
      for (ItemStack combinedStack : combinedStacks) {
        if (invStack.isSimilar(combinedStack)) {
          if (invStack.getAmount() + combinedStack.getAmount() < combinedStack.getMaxStackSize()) {
            combinedStack.setAmount(combinedStack.getAmount() + invStack.getAmount());
            invStack.setAmount(0);
          } else {
            invStack.setAmount(
                invStack.getAmount()
                    - (combinedStack.getMaxStackSize() - combinedStack.getAmount()));
            combinedStack.setAmount(combinedStack.getMaxStackSize());
          }
        }
      }
      if (!invStack.isEmpty()) {
        combinedStacks.add(invStack);
      }
    }

    // sort
    List<ItemStack> sortedItems =
        new ArrayList<>(
            combinedStacks.stream()
                .map(InventoryItem::new)
                .sorted()
                .map(InventoryItem::getItemStack)
                .toList());

    // update container
    List<@Nullable ItemStack> inventoryContents = new ArrayList<>(inventory.getSize());
    for (int i = 0; i < inventory.getSize(); i++) {
      if (i < minSlot || i > maxSlot) {
        inventoryContents.add(inventory.getItem(i));
      } else if (i - minSlot < sortedItems.size()) {
        inventoryContents.add(sortedItems.get(i - minSlot));
      } else {
        inventoryContents.add(null);
      }
    }

    inventory.setContents(inventoryContents.toArray(new ItemStack[0]));

    // notify viewers
    for (HumanEntity viewer : event.getViewers()) {
      if (viewer instanceof Player player) {
        player.updateInventory();
      }
    }
  }
}
