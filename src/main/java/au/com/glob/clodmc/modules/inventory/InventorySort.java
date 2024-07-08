package au.com.glob.clodmc.modules.inventory;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.player.OpAlerts;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
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
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class InventorySort implements Listener, Module {
  private static final Map<String, Integer> materialOrder = new HashMap<>(1477);

  public InventorySort() {
    super();

    List<String> alerts = new ArrayList<>(0);
    List<String> allMaterials = Arrays.stream(Material.values()).map(Enum::name).toList();

    // read inventory_order.txt. format follows.  english name is ignored
    // # comment
    // english name  :MATERIAL
    List<String> orderedMaterials = new ArrayList<>();
    InputStream resourceStream = ClodMC.instance.getResource("inventory_order.txt");
    if (resourceStream == null) {
      throw new RuntimeException("failed to read");
    }
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceStream))) {
      String line;
      int lineNo = 0;
      while ((line = reader.readLine()) != null) {
        lineNo++;
        if (line.isBlank() || line.startsWith("#")) {
          continue;
        }
        String[] parts = line.split(":");
        if (parts.length != 2) {
          alerts.add("inventory_order.txt: invalid line " + lineNo);
          continue;
        }
        orderedMaterials.add(parts[1]);
      }
    } catch (IOException e) {
      ClodMC.logError("inventory_order.txt: " + e.getMessage());
      return;
    }

    // build material --> SortItem mapping
    int index = 0;
    for (String material : orderedMaterials) {
      // must exist
      if (!allMaterials.contains(material)) {
        alerts.add("inventory_order.txt: invalid: " + material);
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
      if (!name.startsWith("LEGACY_") && !materialOrder.containsKey(name)) {
        alerts.add("inventory_order.txt: missing: " + name);
      }
    }

    for (String alert : alerts) {
      ClodMC.logError(alert);
      OpAlerts.addAlert(alert);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
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
    List<ItemStack> inventoryContents = new ArrayList<>(inventory.getSize());
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

  private static class InventoryItem implements Comparable<InventoryItem> {
    private final ItemStack itemStack;
    private final int materialIndex;
    private final String name;
    private final String extra;
    private final int amount;
    private final int damage;

    public InventoryItem(ItemStack itemStack) {
      this.itemStack = itemStack;

      // index into inventory_order.txt
      String material = itemStack.getType().name();
      Integer index = InventorySort.materialOrder.get(material);
      this.materialIndex = Objects.requireNonNullElse(index, 0);

      // visible in-game name
      this.name =
          PlainTextComponentSerializer.plainText().serialize(Component.translatable(itemStack));

      ItemMeta meta = itemStack.getItemMeta();
      if (meta != null) {
        StringJoiner extraJoiner = new StringJoiner(".");

        // enchantment storage (eg. book)
        if (meta instanceof EnchantmentStorageMeta enchantmentStorageMeta) {
          for (Map.Entry<Enchantment, Integer> entry :
              enchantmentStorageMeta.getStoredEnchants().entrySet()) {
            extraJoiner.add(entry.getKey().getKey().getKey());
            extraJoiner.add(String.valueOf(entry.getValue()));
          }
        }

        this.extra = extraJoiner.toString();

        // damage
        if (meta instanceof Damageable damageableMeta) {
          this.damage = damageableMeta.getDamage();
        } else {
          this.damage = 0;
        }
      } else {
        this.extra = "";
        this.damage = 0;
      }

      // amount
      this.amount = itemStack.getAmount();
    }

    @Override
    public String toString() {
      StringJoiner joiner = new StringJoiner(":", "[", "]");
      joiner.add(String.valueOf(this.materialIndex));
      joiner.add(this.name);
      joiner.add(this.extra);
      joiner.add(String.valueOf(this.amount));
      joiner.add(String.valueOf(this.damage));
      return joiner.toString();
    }

    public ItemStack getItemStack() {
      return this.itemStack;
    }

    @Override
    public int compareTo(@NotNull InventorySort.InventoryItem o) {
      int comp;
      // material
      comp = Integer.compare(this.materialIndex, o.materialIndex);
      if (comp != 0) {
        return comp;
      }
      // item description
      comp = this.name.compareTo(o.name);
      if (comp != 0) {
        return comp;
      }
      // item extra
      comp = this.extra.compareTo(o.extra);
      if (comp != 0) {
        return comp;
      }
      // stack size, largest first
      comp = Integer.compare(o.amount, this.amount);
      if (comp != 0) {
        return comp;
      }
      // durability, undamaged first
      return Integer.compare(this.damage, o.damage);
    }
  }
}
