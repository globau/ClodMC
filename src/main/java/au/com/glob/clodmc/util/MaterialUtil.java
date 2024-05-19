package au.com.glob.clodmc.util;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Material;

public class MaterialUtil {
  public static Set<Material> ALWAYS_SAFE;
  public static Set<Material> ALWAYS_UNSAFE;

  public static void init() {
    ALWAYS_SAFE = getMatchingMaterials("DIRT_PATH");
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
