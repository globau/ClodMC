package au.com.glob.clodmc.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Snow;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.BoundingBox;
import org.jspecify.annotations.NullMarked;

/** player teleport helpers */
@NullMarked
public class TeleportUtil {
  private static final int CHECK_RADIUS = 3;
  private static final int MAX_RADIUS = 8;
  private static final int POP_RADIUS = 16;
  private static final Vector3i[] SHIFT_VECTORS;

  static {
    final List<Vector3i> pos = new ArrayList<>();
    for (int x = -CHECK_RADIUS; x <= CHECK_RADIUS; x++) {
      for (int y = -CHECK_RADIUS; y <= CHECK_RADIUS; y++) {
        for (int z = -CHECK_RADIUS; z <= CHECK_RADIUS; z++) {
          pos.add(new Vector3i(x, y, z));
        }
      }
    }
    pos.sort(Comparator.comparingInt((Vector3i a) -> a.x * a.x + a.y * a.y + a.z * a.z));
    SHIFT_VECTORS = pos.toArray(new Vector3i[0]);
  }

  private static final org.bukkit.Color TELEPORT_COLUR_A = org.bukkit.Color.fromRGB(0x00BFFF);
  private static final org.bukkit.Color TELEPORT_COLUR_B = org.bukkit.Color.fromRGB(0xFFFFFF);

  public static void teleport(Player player, Location location, String reason) {
    Location fromLoc = player.getLocation();

    Location destinationLoc;
    if (player.getGameMode().equals(GameMode.CREATIVE)) {
      destinationLoc = location;
    } else {
      destinationLoc = TeleportUtil.getSafePos(location);
      destinationLoc.setYaw(location.getYaw());
      destinationLoc.setPitch(location.getPitch());
    }

    if (fromLoc.getBlockX() == destinationLoc.getBlockX()
        && fromLoc.getBlockY() == destinationLoc.getBlockY()
        && fromLoc.getBlockZ() == destinationLoc.getBlockZ()) {
      Chat.fyi(player, "Teleport not required");
      return;
    }

    String prefix = "Teleporting you ";
    if (player.isInsideVehicle()) {
      prefix =
          switch (player.getVehicle()) {
            case Horse ignored -> "Dismounting your horse and teleporting you ";
            case Boat ignored -> "Dismounting your boat and teleporting you ";
            case null, default -> "Dismounting and teleporting you ";
          };
    }
    Chat.fyi(player, prefix + reason);

    player
        .teleportAsync(destinationLoc, PlayerTeleportEvent.TeleportCause.COMMAND)
        .thenAccept(
            (Boolean result) -> {
              if (result) {
                TeleportUtil.playTeleportSound(fromLoc, player);
                TeleportUtil.playTeleportSound(destinationLoc, player);
                TeleportUtil.showTeleportParticles(fromLoc);
              }
            })
        .exceptionally(
            (Throwable ex) -> {
              Chat.error(player, "Teleport failed");
              return null;
            });
  }

  public static Location getStandingPos(Player player) {
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

  public static Location getSafePos(Location location) {
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
      } else if (Tag.WOOL_CARPETS.isTagged(material) || material == Material.MOSS_CARPET) {
        doubleY -= 15.0 / 16.0;
      }
    } else {
      if (block.getBlockData() instanceof Snow snow) {
        doubleY += (snow.getLayers() - 1) * (1.0 / 8.0);
      }
    }

    // if, after all that, the player doesn't need to be moved to a safe location, return the
    // original location unmodified to retain sub-block precision
    Location safeLocation = new Location(world, doubleX, doubleY, doubleZ, 0, 0);
    if (safeLocation.getBlockX() == location.getBlockX()
        && safeLocation.getBlockY() == location.getBlockY()
        && safeLocation.getBlockY() == location.getBlockY()) {
      return location;
    }

    return safeLocation;
  }

  public static boolean isUnsafe(Block feetBlock) {
    Material feetMaterial = feetBlock.getType();
    Block surfaceBlock = feetBlock.getRelative(BlockFace.DOWN);
    Material surfaceMaterial = surfaceBlock.getType();

    // some materials are never safe to stand on
    if (surfaceMaterial == Material.AIR
        || surfaceMaterial == Material.CACTUS
        || surfaceMaterial == Material.CAMPFIRE
        || surfaceMaterial == Material.CAVE_AIR
        || surfaceMaterial == Material.END_PORTAL
        || surfaceMaterial == Material.FIRE
        || surfaceMaterial == Material.LAVA
        || surfaceMaterial == Material.MAGMA_BLOCK
        || surfaceMaterial == Material.NETHER_PORTAL
        || surfaceMaterial == Material.POWDER_SNOW
        || surfaceMaterial == Material.SOUL_CAMPFIRE
        || surfaceMaterial == Material.SOUL_FIRE
        || surfaceMaterial == Material.SWEET_BERRY_BUSH
        || surfaceMaterial == Material.VOID_AIR
        || surfaceMaterial == Material.WITHER_ROSE) {
      return true;
    }

    // ouch
    if (feetMaterial == Material.FIRE
        || feetMaterial == Material.LAVA
        || feetMaterial == Material.POWDER_SNOW) {
      return true;
    }

    // surface must be solid, blocks where feet and head are mustn't be
    return !canStandOn(surfaceBlock)
        || feetBlock.isSolid()
        || feetBlock.getRelative(BlockFace.UP).isSolid();
  }

  private static boolean canStandOn(Block block) {
    Material material = block.getType();
    if (Tag.WOOL_CARPETS.isTagged(material) || material == Material.SCAFFOLDING) {
      return true;
    }

    return block.isSolid();
  }

  public static Location getRandomLoc(Location loc, int randomRadius) {
    loc = loc.clone();
    int attempts = 0;
    while (attempts <= randomRadius) {
      attempts++;
      Random rand = new Random();
      double angle = rand.nextDouble() * 2 * Math.PI;
      double distance = rand.nextDouble() * randomRadius;
      loc.add(
          (double) Math.round(distance + Math.cos(angle)),
          0,
          (double) Math.round(distance + Math.sin(angle)));
      if (!isUnsafe(loc.getBlock())) {
        break;
      }
    }
    return loc;
  }

  private static void playTeleportSound(Location loc, Player excludedPlayer) {
    for (Player player : loc.getNearbyPlayers(POP_RADIUS)) {
      if (!player.equals(excludedPlayer)) {
        player.playSound(
            loc,
            Sound.UI_HUD_BUBBLE_POP,
            (float) ((1 - player.getLocation().distance(loc) / POP_RADIUS) * 0.75),
            1);
      }
    }
  }

  private static void showTeleportParticles(Location loc) {
    World world = loc.getWorld();
    if (world == null) {
      return;
    }
    Location bottom = loc.clone();
    Location top = loc.clone().add(0, 1, 0);

    Particle.DustTransition dustTransition =
        new Particle.DustTransition(TELEPORT_COLUR_A, TELEPORT_COLUR_B, 1.0F);
    world.spawnParticle(
        Particle.DUST_COLOR_TRANSITION,
        bottom.add(0.5, 0.5, 0.5),
        10,
        0.5,
        1.0,
        0.5,
        0.0,
        dustTransition);
    world.spawnParticle(
        Particle.DUST_COLOR_TRANSITION,
        top.add(0.5, 0.5, 0.5),
        10,
        0.5,
        1.0,
        0.5,
        0.0,
        dustTransition);
  }
}
