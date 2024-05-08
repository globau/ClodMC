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
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Slab;
import org.jetbrains.annotations.NotNull;

public class BlockPos {
  private final World world;
  private int x;
  private int y;
  private int z;
  private double adjustY;

  private static final Material PORTAL = getMaterial("NETHER_PORTAL", "PORTAL");
  private static final Set<Material> UNSAFE_BELOW =
      getMatchingMaterials(
          "CACTUS",
          "CAMPFIRE",
          "FIRE",
          "FLOWING_LAVA",
          "LAVA",
          "MAGMA_BLOCK",
          "SOUL_CAMPFIRE",
          "SOUL_FIRE",
          "STATIONARY_LAVA",
          "SWEET_BERRY_BUSH",
          "WITHER_ROSE");

  private static final int CHECK_RADIUS = 3;
  private static final Vector3D[] SHIFT_VECTORS;

  private record Vector3D(int x, int y, int z) {}

  static {
    final List<Vector3D> pos = new ArrayList<>();
    for (int x = -CHECK_RADIUS; x <= CHECK_RADIUS; x++) {
      for (int y = -CHECK_RADIUS; y <= CHECK_RADIUS; y++) {
        for (int z = -CHECK_RADIUS; z <= CHECK_RADIUS; z++) {
          pos.add(new Vector3D(x, y, z));
        }
      }
    }
    pos.sort(Comparator.comparingInt(a -> a.x * a.x + a.y * a.y + a.z * a.z));
    SHIFT_VECTORS = pos.toArray(new Vector3D[0]);
  }

  private BlockPos(@NotNull World world, int x, int y, int z) {
    this.world = world;
    this.x = x;
    this.y = y;
    this.z = z;
    this.adjustY = 0.0;
  }

  public static BlockPos of(@NotNull Location loc) {
    return new BlockPos(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
  }

  public @NotNull Location asLocation() {
    return new Location(this.world, this.x + 0.5, this.y + this.adjustY, this.z + 0.5);
  }

  private @NotNull Block getBlock() {
    return this.world.getBlockAt(this.x, this.y, this.z);
  }

  private @NotNull Block getBlockBelow() {
    return this.world.getBlockAt(this.x, this.y - 1, this.z);
  }

  private @NotNull Block getBlockAbove() {
    return this.world.getBlockAt(this.x, this.y + 1, this.z);
  }

  public @NotNull BlockPos getSafePos() throws LocationError {
    final int worldMinY = this.world.getMinHeight();
    final int worldLogicalY = this.world.getLogicalHeight();
    final int worldMaxY = this.y < worldLogicalY ? worldLogicalY : this.world.getMaxHeight();

    BlockPos pos = new BlockPos(this.world, this.x, this.y, this.z);

    // push up outside of non-air blocks
    while (!pos.getBlock().isEmpty() && pos.getBlockAbove().isEmpty()) {
      pos.y++;
      if (pos.y == worldMaxY) {
        pos.y = this.y;
      }
    }

    // mid-air isn't safe
    while (pos.getBlockBelow().isEmpty()) {
      pos.y--;
      if (pos.y == worldMinY) {
        pos.y = this.y;
        break;
      }
    }

    // find a safe spot try within 3x3x3 area
    int i = 0;
    while (pos.isUnsafe()) {
      i++;
      if (i >= SHIFT_VECTORS.length) {
        pos.x = this.x;
        pos.y = clamp(this.y + CHECK_RADIUS, worldMinY, worldMaxY);
        pos.z = this.z;
        break;
      }
      pos.x = this.x + SHIFT_VECTORS[i].x;
      pos.y = clamp(this.y + SHIFT_VECTORS[i].y, worldMinY, worldMaxY);
      pos.z = this.z + SHIFT_VECTORS[i].z;
    }

    // stand on top of bottom-slabs
    if (pos.getBlockBelow().getState().getBlockData() instanceof Slab slab
        && slab.getType() == Slab.Type.BOTTOM) {
      pos.adjustY -= 0.5;
    }

    return pos;
  }

  public boolean isUnsafe() {
    Block block = this.getBlock();
    return !block.isEmpty()
        || block.getType() == PORTAL
        || UNSAFE_BELOW.contains(this.getBlockBelow().getType());
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

  private static int clamp(int value, int min, int max) {
    return Math.min(Math.max(value, min), max);
  }
}
