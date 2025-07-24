package au.com.glob.clodmc.modules.interactions.gateways;

import au.com.glob.clodmc.util.StringUtil;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Color;
import org.bukkit.Material;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class Colour {
  final Material material;
  final String name;
  final int index;
  final Color color;

  Colour(Material material, String name, int index, Color color) {
    this.material = material;
    this.name = name;
    this.index = index;
    this.color = color;
  }

  @Override
  public String toString() {
    return this.name;
  }

  String getDisplayName() {
    return StringUtil.toTitleCase(this.name.replace('_', ' '));
  }

  TextComponent getText() {
    return Component.text(this.getDisplayName());
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    Colour that = (Colour) obj;
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
