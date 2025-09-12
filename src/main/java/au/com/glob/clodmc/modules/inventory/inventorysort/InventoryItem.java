package au.com.glob.clodmc.modules.inventory.inventorysort;

import au.com.glob.clodmc.util.StringUtil;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import org.bukkit.MusicInstrument;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MusicInstrumentMeta;
import org.jspecify.annotations.NullMarked;

/** wrapper for itemstack with sorting comparison logic */
@NullMarked
class InventoryItem implements Comparable<InventoryItem> {
  private final ItemStack itemStack;
  private final int materialIndex;
  private final String name;
  private final String extra;
  private final int amount;
  private final int damage;

  InventoryItem(final ItemStack itemStack) {
    this.itemStack = itemStack;

    // index into inventory_order.txt
    final Integer index = InventorySort.materialOrder.get(itemStack.getType().name());
    this.materialIndex = Objects.requireNonNullElse(index, 0);

    // visible in-game name
    this.name = StringUtil.asText(itemStack);

    final ItemMeta meta = itemStack.getItemMeta();
    if (meta != null) {
      final StringJoiner extraJoiner = new StringJoiner(".");

      switch (meta) {
        case final EnchantmentStorageMeta enchantmentStorageMeta -> {
          // enchantment storage (eg. book)
          for (final Map.Entry<Enchantment, Integer> entry :
              enchantmentStorageMeta.getStoredEnchants().entrySet()) {
            extraJoiner.add(StringUtil.asText(entry.getKey()));
            extraJoiner.add(String.valueOf(entry.getValue()));
          }
        }
        case final MusicInstrumentMeta musicInstrumentMeta -> {
          // goat horns
          final MusicInstrument instrument = musicInstrumentMeta.getInstrument();
          if (instrument != null) {
            extraJoiner.add(StringUtil.asText(instrument));
          }
        }
        default -> {
          // nothing
        }
      }

      this.extra = extraJoiner.toString();

      // damage
      this.damage =
          meta instanceof final Damageable damageableMeta ? damageableMeta.getDamage() : 0;
    } else {
      this.extra = "";
      this.damage = 0;
    }

    // amount
    this.amount = itemStack.getAmount();
  }

  @Override
  public String toString() {
    final StringJoiner joiner = new StringJoiner(":", "[", "]");
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
  public int compareTo(final InventoryItem o) {
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
