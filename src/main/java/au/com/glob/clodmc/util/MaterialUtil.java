package au.com.glob.clodmc.util;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public class MaterialUtil {
  @SuppressWarnings("NotNullFieldNotInitialized")
  public static @NotNull Set<Material> ALWAYS_SAFE;

  @SuppressWarnings("NotNullFieldNotInitialized")
  public static @NotNull Set<Material> ALWAYS_UNSAFE;

  public static void init() {
    ALWAYS_SAFE =
        getMatchingMaterials(
            "BLACK_CARPET",
            "BLUE_CARPET",
            "BROWN_CARPET",
            "CYAN_CARPET",
            "GRAY_CARPET",
            "GREEN_CARPET",
            "LIGHT_BLUE_CARPET",
            "LIGHT_GRAY_CARPET",
            "LIME_CARPET",
            "MAGENTA_CARPET",
            "MOSS_CARPET",
            "ORANGE_CARPET",
            "PINK_CARPET",
            "PURPLE_CARPET",
            "RED_CARPET",
            "WHITE_CARPET",
            "YELLOW_CARPET",
            "DIRT_PATH",
            "SCAFFOLDING");
    ALWAYS_UNSAFE =
        getMatchingMaterials(
            "CACTUS",
            "CAMPFIRE",
            "END_PORTAL",
            "FIRE",
            "LAVA",
            "MAGMA_BLOCK",
            "NETHER_PORTAL",
            "SOUL_CAMPFIRE",
            "SOUL_FIRE",
            "SWEET_BERRY_BUSH",
            "WITHER_ROSE");
  }

  private static Set<Material> getMatchingMaterials(final String... names) {
    final Set<Material> materials = EnumSet.noneOf(Material.class);
    for (final String name : names) {
      try {
        final Field enumField = Material.class.getDeclaredField(name);
        if (enumField.isEnumConstant()) {
          materials.add((Material) enumField.get(null));
        }
      } catch (final NoSuchFieldException | IllegalAccessException e) {
        throw new RuntimeException("bad material type: " + name);
      }
    }
    return materials;
  }
}
