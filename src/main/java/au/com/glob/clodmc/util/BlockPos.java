package au.com.glob.clodmc.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public class BlockPos {
  private final World world;
  private int x;
  private int y;
  private int z;

  private static final Set<Material> DAMAGING_TYPES =
      getMatchingMaterials(
          "CACTUS",
          "CAMPFIRE",
          "FIRE",
          "MAGMA_BLOCK",
          "SOUL_CAMPFIRE",
          "SOUL_FIRE",
          "SWEET_BERRY_BUSH",
          "WITHER_ROSE");
  private static final Set<Material> LAVA_TYPES =
      getMatchingMaterials("FLOWING_LAVA", "LAVA", "STATIONARY_LAVA");
  private static final Material PORTAL = getMaterial("NETHER_PORTAL", "PORTAL");
  private static final Material LIGHT = getMaterial("LIGHT");
  private static final Material PATH = getMaterial("GRASS_PATH");
  private static final Material FARMLAND = getMaterial("FARMLAND");
  private static final Set<Material> BEDS;
  private static final Set<Material> HOLLOW_MATERIALS = EnumSet.noneOf(Material.class);

  private static final int RADIUS = 3;
  private static final Vector3D[] VOLUME;

  private record Vector3D(int x, int y, int z) {}

  static {
    for (final Material mat : Material.values()) {
      if (!mat.isOccluding()) {
        HOLLOW_MATERIALS.add(mat);
      }
    }
    HOLLOW_MATERIALS.remove(Material.BARRIER);
    HOLLOW_MATERIALS.remove(PATH);
    HOLLOW_MATERIALS.remove(FARMLAND);
    if (LIGHT != null) {
      HOLLOW_MATERIALS.add(LIGHT);
    }

    BEDS =
        getMatchingMaterials(
            "BED",
            "BED_BLOCK",
            "WHITE_BED",
            "ORANGE_BED",
            "MAGENTA_BED",
            "LIGHT_BLUE_BED",
            "YELLOW_BED",
            "LIME_BED",
            "PINK_BED",
            "GRAY_BED",
            "LIGHT_GRAY_BED",
            "CYAN_BED",
            "PURPLE_BED",
            "BLUE_BED",
            "BROWN_BED",
            "GREEN_BED",
            "RED_BED",
            "BLACK_BED");

    final List<Vector3D> pos = new ArrayList<>();
    for (int x = -RADIUS; x <= RADIUS; x++) {
      for (int y = -RADIUS; y <= RADIUS; y++) {
        for (int z = -RADIUS; z <= RADIUS; z++) {
          pos.add(new Vector3D(x, y, z));
        }
      }
    }
    pos.sort(Comparator.comparingInt(a -> a.x * a.x + a.y * a.y + a.z * a.z));
    VOLUME = pos.toArray(new Vector3D[0]);
  }

  private BlockPos(@NotNull World world, int x, int y, int z) {
    this.world = world;
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public static BlockPos of(@NotNull Location loc) {
    return new BlockPos(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
  }

  public @NotNull Location asLocation() {
    return new Location(this.world, this.x, this.y, this.z);
  }

  public @NotNull BlockPos getSafePos() throws LocationError {
    final int worldMinY = this.world.getMinHeight();
    final int worldLogicalY = this.world.getLogicalHeight();
    final int worldMaxY = this.y < worldLogicalY ? worldLogicalY : this.world.getMaxHeight();

    BlockPos pos = new BlockPos(this.world, this.x, this.y, this.z);

    pos.moveInsideWorldBorder();

    // put on ground
    while (pos.isAboveAir()) {
      pos.y--;
      if (pos.y < 0) {
        pos.y = this.y;
        break;
      }
    }

    // try nearby
    if (pos.isUnsafe()) {
      pos.x = pos.x == this.x ? pos.x - 1 : pos.x + 1;
      pos.z = pos.z == this.z ? pos.z - 1 : pos.z + 1;
    }

    // try within 3x3x3 area
    int i = 0;
    while (pos.isUnsafe()) {
      i++;
      if (i >= VOLUME.length) {
        pos.x = this.x;
        pos.y = clamp(this.y + RADIUS, worldMinY, worldMaxY);
        pos.z = this.z;
        break;
      }
      pos.x = this.x + VOLUME[i].x;
      pos.y = clamp(this.y + VOLUME[i].y, worldMinY, worldMaxY);
      pos.z = this.z + VOLUME[i].z;
    }

    // move up
    while (pos.isUnsafe()) {
      pos.y++;
      if (pos.y >= worldMaxY) {
        pos.x++;
        break;
      }
    }

    // move down
    while (pos.isUnsafe()) {
      pos.y--;
      if (pos.y <= worldMinY + 1) {
        pos.x++;
        // Allow spawning at the top of the world, but not above the nether roof
        pos.y = Math.min(this.world.getHighestBlockYAt(pos.x, pos.z) + 1, worldMaxY);
        if (pos.x - 48 > this.x) {
          throw new LocationError("Hole in floor!");
        }
      }
    }

    return pos;
  }

  private void moveInsideWorldBorder() {
    Location center = this.world.getWorldBorder().getCenter();
    int radius = (int) this.world.getWorldBorder().getSize() / 2;

    int x1 = center.getBlockX() - radius;
    int x2 = center.getBlockX() + radius;
    if (this.x < x1) {
      this.x = x1;
    } else if (this.x > x2) {
      this.x = x2;
    }

    int z1 = center.getBlockZ() - radius;
    int z2 = center.getBlockZ() + radius;
    if (this.z < z1) {
      this.z = z1;
    } else if (this.z > z2) {
      this.z = z2;
    }
  }

  public boolean isUnsafe() {
    return this.isDamaging() || this.isAboveAir();
  }

  private boolean isDamaging() {
    final Material block = this.world.getBlockAt(this.x, this.y, this.z).getType();
    final Material below = this.world.getBlockAt(this.x, this.y - 1, this.z).getType();
    final Material above = this.world.getBlockAt(this.x, this.y + 1, this.z).getType();
    if (DAMAGING_TYPES.contains(below) || LAVA_TYPES.contains(below) || isBed(below)) {
      return true;
    }
    if (block == PORTAL) {
      return true;
    }
    return !HOLLOW_MATERIALS.contains(block) || !HOLLOW_MATERIALS.contains(above);
  }

  private boolean isAboveAir() {
    return this.y > this.world.getMaxHeight()
        || HOLLOW_MATERIALS.contains(this.world.getBlockAt(this.x, this.y - 1, this.z).getType());
  }

  public static class LocationError extends Exception {
    public LocationError(String message) {
      super(message);
    }
  }

  private static <T extends Enum<T>> Set<T> getMatchingMaterials(final String... names) {
    //noinspection unchecked
    final Set<T> set = EnumSet.noneOf((Class<T>) Material.class);
    for (final String name : names) {
      try {
        final Field enumField = Material.class.getDeclaredField(name);
        if (enumField.isEnumConstant()) {
          //noinspection unchecked
          set.add((T) enumField.get(null));
        }
      } catch (final NoSuchFieldException | IllegalAccessException e) {
        // ignored
      }
    }
    return set;
  }

  private static <T extends Enum<T>> T valueOfMaterial(final String... names) {
    for (final String name : names) {
      try {
        final Field enumField = Material.class.getDeclaredField(name);
        if (enumField.isEnumConstant()) {
          //noinspection unchecked
          return (T) enumField.get(null);
        }
      } catch (final NoSuchFieldException | IllegalAccessException e) {
        // ignored
      }
    }
    return null;
  }

  private static Material getMaterial(final String... names) {
    return valueOfMaterial(names);
  }

  private static boolean isBed(final Material material) {
    return BEDS.contains(material);
  }

  private static int clamp(int value, int min, int max) {
    return Math.min(Math.max(value, min), max);
  }
}
