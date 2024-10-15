package au.com.glob.clodmc.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Snow;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

/** player teleport helpers */
public class TeleportUtil {
  private static final int CHECK_RADIUS = 3;
  private static final int MAX_RADIUS = 8;
  private static final Vector3D @NotNull [] SHIFT_VECTORS;

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

  public static @NotNull Location getStandingPos(@NotNull Player player) {
    // player.getLocation() returns the centre of the player; if the player
    // is standing on the edge of a block, their centre will be over a different
    // block from what they are standing on.
    World world = player.getWorld();
    BoundingBox boundingBox = player.getBoundingBox();
    for (int x = (int) Math.floor(boundingBox.getMinX());
        x <= Math.floor(boundingBox.getMaxX());
        x++) {
      for (int z = (int) Math.floor(boundingBox.getMinZ());
          z <= Math.floor(boundingBox.getMaxZ());
          z++) {
        Block block = world.getBlockAt(x, (int) Math.floor(player.getLocation().getY() - 0.01), z);
        if (block.getType().isSolid()) {
          Location location = player.getLocation();
          location.setX((double) block.getX() + 0.5);
          location.setZ((double) block.getZ() + 0.5);
          return location;
        }
      }
    }

    return player.getLocation();
  }

  public static @NotNull Location getSafePos(@NotNull Location location) {
    World world = location.getWorld();
    int x = location.getBlockX();
    int y = location.getBlockY();
    int z = location.getBlockZ();

    final int worldMinY = world.getMinHeight();
    final int worldLogicalY = world.getLogicalHeight();
    final int worldMaxY = y < worldLogicalY ? worldLogicalY : world.getMaxHeight();

    // push up outside of solid blocks
    while (world.getBlockAt(x, y, z).isSolid()) {
      y++;
    }

    // push up outside of unsafe blocks
    while (isUnsafe(world.getBlockAt(x, y, z))) {
      if (y == worldMaxY) {
        y = location.getBlockY();
        break;
      }
      y++;
    }

    // avoid pushing up too far
    if (y - location.getBlockY() > MAX_RADIUS) {
      y = location.getBlockY();
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
    while (isUnsafe(world.getBlockAt(x, y, z))) {
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
    Block block = world.getBlockAt(x, y, z);
    if (block.isEmpty()) {
      Block blockDown = world.getBlockAt(x, y - 1, z);
      Material material = blockDown.getType();
      if (blockDown.getBlockData() instanceof Slab slab && slab.getType() == Slab.Type.BOTTOM) {
        doubleY -= 0.5;
      } else if (Tag.FENCES.isTagged(material)
          || Tag.FENCE_GATES.isTagged(material)
          || Tag.WALLS.isTagged(material)) {
        doubleY += 0.5;
      } else if (material == Material.DIRT_PATH) {
        doubleY -= 1.0 / 16.0;
      }
    } else {
      Material material = block.getType();
      if (Tag.WOOL_CARPETS.isTagged(material) || material == Material.MOSS_CARPET) {
        doubleY += 1.0 / 16.0;
      } else if (block.getBlockData() instanceof Snow snow) {
        doubleY += (snow.getLayers() - 1) * (1.0 / 8.0);
      }
    }

    return new Location(world, doubleX, doubleY, doubleZ, 0, 0);
  }

  public static boolean isUnsafe(@NotNull Block feetBlock) {
    Material feetMaterial = feetBlock.getType();
    Block surfaceBlock = feetBlock.getRelative(BlockFace.DOWN);
    Material surfaceMaterial = surfaceBlock.getType();

    // some materials are never safe to stand on
    if (surfaceMaterial == Material.AIR
        || surfaceMaterial == Material.CACTUS
        || surfaceMaterial == Material.CAMPFIRE
        || surfaceMaterial == Material.END_PORTAL
        || surfaceMaterial == Material.FIRE
        || surfaceMaterial == Material.LAVA
        || surfaceMaterial == Material.MAGMA_BLOCK
        || surfaceMaterial == Material.NETHER_PORTAL
        || surfaceMaterial == Material.POWDER_SNOW
        || surfaceMaterial == Material.SOUL_CAMPFIRE
        || surfaceMaterial == Material.SOUL_FIRE
        || surfaceMaterial == Material.SWEET_BERRY_BUSH
        || surfaceMaterial == Material.WITHER_ROSE) {
      return true;
    }

    // ouch
    if (feetMaterial == Material.FIRE
        || feetMaterial == Material.LAVA
        || feetMaterial == Material.POWDER_SNOW) {
      return true;
    }

    // the player stands inside blocks that aren't fully solid
    boolean feetIsSolid;
    if (Tag.WOOL_CARPETS.isTagged(feetMaterial)
        || feetMaterial == Material.MOSS_CARPET
        || Tag.SLABS.isTagged(feetMaterial)
        || feetMaterial == Material.SNOW
        || feetMaterial == Material.SCAFFOLDING
        || feetMaterial == Material.DIRT_PATH) {
      feetIsSolid = false;
    } else {
      feetIsSolid = feetBlock.isSolid();
    }

    // surface must be solid, blocks where feet and head are mustn't be
    return !surfaceBlock.isSolid() || feetIsSolid || feetBlock.getRelative(BlockFace.UP).isSolid();
  }

  public static @NotNull Location getRandomLoc(@NotNull Location loc, int randomRadius) {
    loc = loc.clone();
    int attempts = 0;
    while (attempts <= randomRadius) {
      attempts++;
      Random rand = new Random();
      double angle = rand.nextDouble() * 2 * Math.PI;
      double distance = rand.nextDouble() * randomRadius;
      loc.add(Math.round(distance + Math.cos(angle)), 0, Math.round(distance + Math.sin(angle)));
      if (!isUnsafe(loc.getBlock())) {
        break;
      }
    }
    return loc;
  }
}
