package au.com.glob.clodmc.modules.interactions.gateways;

import au.com.glob.clodmc.util.StringUtil;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Color;
import org.bukkit.Material;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** represents a gateway colour with material and display properties */
@NullMarked
final class Colour {
  final Material material;
  final String name;
  final int index;
  final Color color;

  // creates a colour with material, name, index and colour
  Colour(final Material material, final String name, final int index, final Color color) {
    this.material = material;
    this.name = name;
    this.index = index;
    this.color = color;
  }

  @Override
  public String toString() {
    return this.name;
  }

  // gets formatted display name for ui
  String getDisplayName() {
    return StringUtil.toTitleCase(this.name.replace('_', ' '));
  }

  // gets text component for chat display
  TextComponent getText() {
    return Component.text(this.getDisplayName());
  }

  @Override
  public boolean equals(@Nullable final Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    final Colour that = (Colour) obj;
    return Objects.equals(this.material, that.material)
        && Objects.equals(this.name, that.name)
        && this.index == that.index
        && Objects.equals(this.color, that.color);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.material, this.name, this.index, this.color);
  }
}
