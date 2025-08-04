package au.com.glob.clodmc.modules.inventory.deeppockets;

import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** represents an item stack location within a shulker box */
@NullMarked
final class ShulkerItemStack {
  final int playerSlot;
  final int shulkerSlot;
  final int amount;

  ShulkerItemStack(int playerSlot, int shulkerSlot, int amount) {
    this.playerSlot = playerSlot;
    this.shulkerSlot = shulkerSlot;
    this.amount = amount;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    ShulkerItemStack that = (ShulkerItemStack) obj;
    return this.playerSlot == that.playerSlot
        && this.shulkerSlot == that.shulkerSlot
        && this.amount == that.amount;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.playerSlot, this.shulkerSlot, this.amount);
  }
}
