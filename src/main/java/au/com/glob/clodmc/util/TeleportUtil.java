package au.com.glob.clodmc.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Snow;
import org.jetbrains.annotations.NotNull;

public class TeleportUtil {
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
    pos.sort(Comparator.comparingInt((Vector3D a) -> a.x * a.x + a.y * a.y + a.z * a.z));
    SHIFT_VECTORS = pos.toArray(new Vector3D[0]);
  }

  public static @NotNull Location getSafePos(@NotNull Location location) {
    World world = location.getWorld();
    int x = location.getBlockX();
    int y = location.getBlockY();
    int z = location.getBlockZ();

    final int worldMinY = world.getMinHeight();
    final int worldLogicalY = world.getLogicalHeight();
    final int worldMaxY = y < worldLogicalY ? worldLogicalY : world.getMaxHeight();

    // push up outside of unsafe blocks
    while (isUnsafe(world.getBlockAt(x, y, z), true)
        && !isUnsafe(world.getBlockAt(x, y + 1, z), true)) {
      y++;
      if (y == worldMaxY) {
        y = location.getBlockY();
      }
    }

    // mid-air isn't safe
    while (world.getBlockAt(x, y - 1, z).isEmpty()) {
      y--;
      if (y == worldMinY) {
        y = location.getBlockY();
        break;
      }
    }

    // find a safe spot try within 3x3x3 area
    int i = 0;
    while (isUnsafe(world.getBlockAt(x, y, z), true)) {
      i++;
      if (i >= SHIFT_VECTORS.length) {
        x = location.getBlockX();
        y = Math.clamp(location.getBlockY() + CHECK_RADIUS, worldMinY, worldMaxY);
        z = location.getBlockZ();
        break;
      }
      x = location.getBlockX() + SHIFT_VECTORS[i].x;
      y = Math.clamp(location.getBlockY() + SHIFT_VECTORS[i].y, worldMinY, worldMaxY);
      z = location.getBlockZ() + SHIFT_VECTORS[i].z;
    }

    double doubleX = x + 0.5;
    double doubleY = y;
    double doubleZ = z + 0.5;

    // handle standing on blocks that aren't 1.0 high
    Block block = world.getBlockAt(x, y - 1, z);
    BlockData blockData = block.getState().getBlockData();
    Material material = block.getType();
    if (blockData instanceof Slab slab && slab.getType() == Slab.Type.BOTTOM) {
      doubleY -= 0.5;
    } else if (blockData instanceof Snow snow) {
      doubleY += snow.getLayers() * (1.0 / 8.0) - 1;
    } else if (Tag.WOOL_CARPETS.isTagged(material)) {
      doubleY += 0.0625 - 1;
    } else if (Tag.FENCES.isTagged(material)
        || Tag.FENCE_GATES.isTagged(material)
        || Tag.WALLS.isTagged(material)) {
      doubleY += 0.5;
    }

    return new Location(world, doubleX, doubleY, doubleZ, 0, 0);
  }

  public static boolean isUnsafe(@NotNull Block block, boolean slabsAreUnsafe) {
    return (!MaterialUtil.ALWAYS_SAFE.contains(block.getType())
            || (slabsAreUnsafe && block.getState().getBlockData() instanceof Slab))
        && (MaterialUtil.ALWAYS_UNSAFE.contains(block.getType())
            || block.isSolid()
            || MaterialUtil.ALWAYS_UNSAFE.contains(block.getRelative(BlockFace.DOWN).getType())
            || block.getRelative(BlockFace.UP).isSolid());
  }
}
