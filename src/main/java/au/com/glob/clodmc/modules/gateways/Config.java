package au.com.glob.clodmc.modules.gateways;

import java.util.List;
import java.util.Map;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

public class Config {
  protected static final @NotNull NamespacedKey RECIPE_KEY = new NamespacedKey("clod-mc", "anchor");

  protected static final @NotNull String DEFAULT_ANCHOR_NAME = "Gateway Anchor";

  protected static final @NotNull NamespacedKey NETWORK_KEY =
      new NamespacedKey("clod-mc", "network");
  protected static final @NotNull NamespacedKey TOP_KEY =
      new NamespacedKey("clod-mc", "network-top");
  protected static final @NotNull NamespacedKey BOTTOM_KEY =
      new NamespacedKey("clod-mc", "network-bottom");

  protected static final String @NotNull [] SHAPE = new String[] {"PWP", "EWE", "ERE"};
  protected static final @NotNull Map<Character, Material> SHAPE_MATERIALS =
      Map.of(
          'P', Material.ENDER_PEARL,
          'W', Material.AIR,
          'E', Material.END_STONE,
          'R', Material.RESPAWN_ANCHOR);

  protected static final @NotNull Map<Material, String> NETWORK_CRAFT =
      Map.ofEntries(
          Map.entry(Material.WHITE_WOOL, "white"),
          Map.entry(Material.ORANGE_WOOL, "orange"),
          Map.entry(Material.MAGENTA_WOOL, "magenta"),
          Map.entry(Material.LIGHT_BLUE_WOOL, "light_blue"),
          Map.entry(Material.YELLOW_WOOL, "yellow"),
          Map.entry(Material.LIME_WOOL, "lime"),
          Map.entry(Material.PINK_WOOL, "pink"),
          Map.entry(Material.GRAY_WOOL, "gray"),
          Map.entry(Material.LIGHT_GRAY_WOOL, "light_gray"),
          Map.entry(Material.CYAN_WOOL, "cyan"),
          Map.entry(Material.PURPLE_WOOL, "purple"),
          Map.entry(Material.BLUE_WOOL, "blue"),
          Map.entry(Material.BROWN_WOOL, "brown"),
          Map.entry(Material.GREEN_WOOL, "green"),
          Map.entry(Material.RED_WOOL, "red"),
          Map.entry(Material.BLACK_WOOL, "black"));

  protected static final @NotNull List<String> COLOUR_INDEX =
      List.of(
          "white",
          "orange",
          "magenta",
          "light_blue",
          "yellow",
          "lime",
          "pink",
          "gray",
          "light_gray",
          "cyan",
          "purple",
          "blue",
          "brown",
          "green",
          "red",
          "black");

  protected static final @NotNull List<Color> PARTICLE_COLOURS =
      List.of(
          Color.fromRGB(0xf9ffff),
          Color.fromRGB(0xf9801d),
          Color.fromRGB(0xc64fbd),
          Color.fromRGB(0x3ab3da),
          Color.fromRGB(0xffd83d),
          Color.fromRGB(0x80c71f),
          Color.fromRGB(0xf38caa),
          Color.fromRGB(0x474f52),
          Color.fromRGB(0x9c9d97),
          Color.fromRGB(0x169c9d),
          Color.fromRGB(0x8932b7),
          Color.fromRGB(0x3c44a9),
          Color.fromRGB(0x825432),
          Color.fromRGB(0x5d7c15),
          Color.fromRGB(0xb02e26),
          Color.fromRGB(0x1d1c21));

  protected static int coloursToId(@NotNull String topColour, @NotNull String bottomColour) {
    return (COLOUR_INDEX.indexOf(topColour) << 4) | COLOUR_INDEX.indexOf(bottomColour);
  }

  protected static @NotNull Color idToTopColour(int id) {
    return PARTICLE_COLOURS.get((id >> 4) & 0x0F);
  }

  protected static @NotNull Color idToBottomColour(int id) {
    return PARTICLE_COLOURS.get(id & 0x0F);
  }

  protected static @NotNull String idToTopName(int id) {
    return COLOUR_INDEX.get((id >> 4) & 0x0F);
  }

  protected static @NotNull String idToBottomName(int id) {
    return COLOUR_INDEX.get(id & 0x0F);
  }
}
