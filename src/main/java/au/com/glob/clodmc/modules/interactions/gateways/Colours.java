package au.com.glob.clodmc.modules.interactions.gateways;

import java.util.List;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class Colours {
  static final List<Colour> COLOURS =
      List.of(
          new Colour(Material.WHITE_WOOL, "white", 0, Color.fromRGB(0xf9ffff)),
          new Colour(Material.ORANGE_WOOL, "orange", 1, Color.fromRGB(0xf9801d)),
          new Colour(Material.MAGENTA_WOOL, "magenta", 2, Color.fromRGB(0xc64fbd)),
          new Colour(Material.LIGHT_BLUE_WOOL, "light_blue", 3, Color.fromRGB(0x3ab3da)),
          new Colour(Material.YELLOW_WOOL, "yellow", 4, Color.fromRGB(0xffd83d)),
          new Colour(Material.LIME_WOOL, "lime", 5, Color.fromRGB(0x80c71f)),
          new Colour(Material.PINK_WOOL, "pink", 6, Color.fromRGB(0xf38caa)),
          new Colour(Material.GRAY_WOOL, "gray", 7, Color.fromRGB(0x474f52)),
          new Colour(Material.LIGHT_GRAY_WOOL, "light_gray", 8, Color.fromRGB(0x9c9d97)),
          new Colour(Material.CYAN_WOOL, "cyan", 9, Color.fromRGB(0x169c9d)),
          new Colour(Material.PURPLE_WOOL, "purple", 10, Color.fromRGB(0x8932b7)),
          new Colour(Material.BLUE_WOOL, "blue", 11, Color.fromRGB(0x3c44a9)),
          new Colour(Material.BROWN_WOOL, "brown", 12, Color.fromRGB(0x825432)),
          new Colour(Material.GREEN_WOOL, "green", 13, Color.fromRGB(0x5d7c15)),
          new Colour(Material.RED_WOOL, "red", 14, Color.fromRGB(0xb02e26)),
          new Colour(Material.BLACK_WOOL, "black", 15, Color.fromRGB(0x1d1c21)));

  static @Nullable Colour of(Material material) {
    for (Colour colour : COLOURS) {
      if (colour.material == material) {
        return colour;
      }
    }
    return null;
  }

  static @Nullable Colour of(int index) {
    for (Colour colour : COLOURS) {
      if (colour.index == index) {
        return colour;
      }
    }
    return null;
  }

  static @Nullable Colour of(@Nullable ItemStack item) {
    return item == null ? null : of(item.getType());
  }
}
