package au.com.glob.clodmc.modules.interactions.gateways;

import java.util.List;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** manages available gateway colours and their material mappings */
@NullMarked
public final class Colours {
  private Colours() {}

  static final List<Colour> COLOURS =
      List.of(
          new Colour(
              Material.WHITE_WOOL,
              Material.WHITE_STAINED_GLASS,
              "white",
              0,
              Color.fromRGB(0xf9ffff)),
          new Colour(
              Material.ORANGE_WOOL,
              Material.ORANGE_STAINED_GLASS,
              "orange",
              1,
              Color.fromRGB(0xf9801d)),
          new Colour(
              Material.MAGENTA_WOOL,
              Material.MAGENTA_STAINED_GLASS,
              "magenta",
              2,
              Color.fromRGB(0xc64fbd)),
          new Colour(
              Material.LIGHT_BLUE_WOOL,
              Material.LIGHT_BLUE_STAINED_GLASS,
              "light_blue",
              3,
              Color.fromRGB(0x3ab3da)),
          new Colour(
              Material.YELLOW_WOOL,
              Material.YELLOW_STAINED_GLASS,
              "yellow",
              4,
              Color.fromRGB(0xffd83d)),
          new Colour(
              Material.LIME_WOOL, Material.LIME_STAINED_GLASS, "lime", 5, Color.fromRGB(0x80c71f)),
          new Colour(
              Material.PINK_WOOL, Material.PINK_STAINED_GLASS, "pink", 6, Color.fromRGB(0xf38caa)),
          new Colour(
              Material.GRAY_WOOL, Material.GRAY_STAINED_GLASS, "gray", 7, Color.fromRGB(0x474f52)),
          new Colour(
              Material.LIGHT_GRAY_WOOL,
              Material.LIGHT_GRAY_STAINED_GLASS,
              "light_gray",
              8,
              Color.fromRGB(0x9c9d97)),
          new Colour(
              Material.CYAN_WOOL, Material.CYAN_STAINED_GLASS, "cyan", 9, Color.fromRGB(0x169c9d)),
          new Colour(
              Material.PURPLE_WOOL,
              Material.PURPLE_STAINED_GLASS,
              "purple",
              10,
              Color.fromRGB(0x8932b7)),
          new Colour(
              Material.BLUE_WOOL, Material.BLUE_STAINED_GLASS, "blue", 11, Color.fromRGB(0x3c44a9)),
          new Colour(
              Material.BROWN_WOOL,
              Material.BROWN_STAINED_GLASS,
              "brown",
              12,
              Color.fromRGB(0x825432)),
          new Colour(
              Material.GREEN_WOOL,
              Material.GREEN_STAINED_GLASS,
              "green",
              13,
              Color.fromRGB(0x5d7c15)),
          new Colour(
              Material.RED_WOOL, Material.RED_STAINED_GLASS, "red", 14, Color.fromRGB(0xb02e26)),
          new Colour(
              Material.BLACK_WOOL,
              Material.BLACK_STAINED_GLASS,
              "black",
              15,
              Color.fromRGB(0x1d1c21)));

  // finds colour by material
  static @Nullable Colour of(final Material material) {
    for (final Colour colour : COLOURS) {
      if (colour.material == material) {
        return colour;
      }
    }
    return null;
  }

  // finds colour by index
  static @Nullable Colour of(final int index) {
    for (final Colour colour : COLOURS) {
      if (colour.index == index) {
        return colour;
      }
    }
    return null;
  }

  // finds colour by item material
  static @Nullable Colour of(@Nullable final ItemStack item) {
    return item == null ? null : of(item.getType());
  }
}
