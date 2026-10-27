package au.com.glob.clodmc.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.BoundingBox;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** player teleport helpers */
@NullMarked
public final class TeleportUtil {
  private TeleportUtil() {}

  private static final int CHECK_RADIUS = 3;
  private static final int MAX_RADIUS = 8;
  private static final int SOUND_RADIUS = 16;
  private static final double PLAYER_WIDTH = 0.6;
  private static final double PLAYER_HEIGHT = 1.8;
  private static final double SUPPORT_DEPTH = 0.01;
  private static final Set<Material> DANGEROUS_MATERIALS =
      Set.of(
          Material.CACTUS,
          Material.CAMPFIRE,
          Material.END_GATEWAY,
          Material.END_PORTAL,
          Material.FIRE,
          Material.LAVA,
          Material.MAGMA_BLOCK,
          Material.NETHER_PORTAL,
          Material.POWDER_SNOW,
          Material.SOUL_CAMPFIRE,
          Material.SOUL_FIRE,
          Material.SWEET_BERRY_BUSH,
          Material.WITHER_ROSE);
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
    pos.sort(Comparator.comparingInt((final Vector3i a) -> a.x * a.x + a.y * a.y + a.z * a.z));
    SHIFT_VECTORS = pos.toArray(new Vector3i[0]);
  }

  private static final org.bukkit.Color TELEPORT_COLUR_A = org.bukkit.Color.fromRGB(0x00BFFF);
  private static final org.bukkit.Color TELEPORT_COLUR_B = org.bukkit.Color.fromRGB(0xFFFFFF);

  // teleport player safely with visual and audio effects
  public static void teleport(final Player player, final Location location, final String reason) {
    teleport(player, location, reason, null);
  }

  // teleport player safely with visual and audio effects, with async callback
  public static void teleport(
      final Player player,
      final Location location,
      final String reason,
      @Nullable final Runnable callback) {
    final Location fromLoc = player.getLocation();

    final Location destinationLoc;
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
            case final Horse _ -> "Dismounting your horse and teleporting you ";
            case final Boat _ -> "Dismounting your boat and teleporting you ";
            case null, default -> "Dismounting and teleporting you ";
          };
    }
    Chat.fyi(player, "%s%s".formatted(prefix, reason));

    player
        .teleportAsync(destinationLoc, PlayerTeleportEvent.TeleportCause.COMMAND)
        .thenAccept(
            (final Boolean result) -> {
              if (result) {
                TeleportUtil.playTeleportSoundNearby(fromLoc, destinationLoc, player);
                TeleportUtil.showTeleportParticles(fromLoc);
                if (callback != null) {
                  callback.run();
                }
              }
            })
        .exceptionally(
            (final Throwable _) -> {
              Chat.error(player, "Teleport failed");
              return null;
            });
  }

  // get location of the block the player is standing on
  public static Location getStandingPos(final Player player) {
    // player.getLocation() returns the centre of the player; if the player
    // is standing on the edge of a block, their centre will be over a different
    // block from what they are standing on.
    final World world = player.getWorld();
    final BoundingBox boundingBox = player.getBoundingBox();
    for (int x = (int) Math.floor(boundingBox.getMinX());
        x <= Math.floor(boundingBox.getMaxX());
        x++) {
      for (int z = (int) Math.floor(boundingBox.getMinZ());
          z <= Math.floor(boundingBox.getMaxZ());
          z++) {
        final Block block =
            world.getBlockAt(x, (int) Math.floor(player.getLocation().getY() - 0.01), z);
        if (block.getType().isSolid()) {
          final Location location = player.getLocation();
          location.setX((double) block.getX() + 0.5);
          location.setZ((double) block.getZ() + 0.5);
          return location;
        }
      }
    }

    return player.getLocation();
  }

  // find nearest safe position for teleportation
  public static Location getSafePos(final Location location) {
    if (isSafeAt(location)) {
      return location;
    }

    final World world = location.getWorld();
    int x = location.getBlockX();
    int y = location.getBlockY();
    int z = location.getBlockZ();

    final int worldMinY = world.getMinHeight();
    final int worldLogicalY = world.getLogicalHeight();
    final int worldMaxY = y < worldLogicalY ? worldLogicalY : world.getMaxHeight();

    // push up out of blocks and unsafe locations, without pushing up too far
    final int pushMaxY = Math.min(worldMaxY, location.getBlockY() + MAX_RADIUS);
    while (y < pushMaxY && isUnsafe(world.getBlockAt(x, y, z))) {
      y++;
    }
    if (isUnsafe(world.getBlockAt(x, y, z))) {
      y = location.getBlockY();
    }

    // mid-air isn't safe; fall through open space until there's a floor or a liquid
    while (true) {
      final Block block = world.getBlockAt(x, y, z);
      if (!block.getCollisionShape().getBoundingBoxes().isEmpty()
          || isValidFloor(floorHeight(block))
          || block.getRelative(BlockFace.DOWN).isLiquid()) {
        break;
      }
      y--;
      if (y == worldMinY) {
        y = location.getBlockY();
        break;
      }
    }

    // find a safe spot within a 3x3x3 area
    final int baseX = x;
    final int baseY = y;
    final int baseZ = z;
    int i = 0;
    while (isUnsafe(world.getBlockAt(x, y, z))) {
      i++;
      if (i >= SHIFT_VECTORS.length) {
        x = baseX;
        y = Math.clamp(baseY + CHECK_RADIUS, worldMinY, worldMaxY);
        z = baseZ;
        break;
      }
      x = baseX + SHIFT_VECTORS[i].x;
      y = Math.clamp(baseY + SHIFT_VECTORS[i].y, worldMinY, worldMaxY);
      z = baseZ + SHIFT_VECTORS[i].z;
    }

    final double floor = floorHeight(world.getBlockAt(x, y, z));
    final double doubleY = isValidFloor(floor) ? y + floor : y;
    return new Location(world, x + 0.5, doubleY, z + 0.5, 0, 0);
  }

  // check if the player can stand at the exact location, without centring on a block
  private static boolean isSafeAt(final Location location) {
    final World world = location.getWorld();
    final BoundingBox playerBox = playerBoxAt(location.getX(), location.getY(), location.getZ());
    final BoundingBox supportBox =
        new BoundingBox(
            playerBox.getMinX(),
            playerBox.getMinY() - SUPPORT_DEPTH,
            playerBox.getMinZ(),
            playerBox.getMaxX(),
            playerBox.getMinY(),
            playerBox.getMaxZ());

    final BoundingBox touchingBox = playerBox.clone().union(supportBox);
    for (final Block block : blocksIn(world, touchingBox)) {
      if (DANGEROUS_MATERIALS.contains(block.getType())) {
        return false;
      }
    }

    boolean supported = false;
    for (final Block block : collisionCandidates(world, touchingBox)) {
      if (block.getType() != Material.SCAFFOLDING && collides(block, playerBox)) {
        return false;
      }
      supported |= collides(block, supportBox);
    }
    return supported;
  }

  // check if block location is unsafe for player
  public static boolean isUnsafe(final Block feetBlock) {
    final double floor = floorHeight(feetBlock);
    if (!isValidFloor(floor)) {
      return true;
    }

    if (DANGEROUS_MATERIALS.contains(feetBlock.getType())) {
      return true;
    }
    if (floor <= 0.0
        && DANGEROUS_MATERIALS.contains(feetBlock.getRelative(BlockFace.DOWN).getType())) {
      return true;
    }

    final BoundingBox playerBox =
        playerBoxAt(feetBlock.getX() + 0.5, feetBlock.getY() + floor, feetBlock.getZ() + 0.5);
    for (final Block block : collisionCandidates(feetBlock.getWorld(), playerBox)) {
      if (block.getType() != Material.SCAFFOLDING && collides(block, playerBox)) {
        return true;
      }
    }
    return false;
  }

  private static BoundingBox playerBoxAt(final double x, final double y, final double z) {
    return new BoundingBox(
        x - PLAYER_WIDTH / 2,
        y,
        z - PLAYER_WIDTH / 2,
        x + PLAYER_WIDTH / 2,
        y + PLAYER_HEIGHT,
        z + PLAYER_WIDTH / 2);
  }

  // blocks which the box intersects, excluding those it only touches
  private static List<Block> blocksIn(final World world, final BoundingBox box) {
    final List<Block> blocks = new ArrayList<>();
    for (int x = (int) Math.floor(box.getMinX()); x < Math.ceil(box.getMaxX()); x++) {
      for (int y = (int) Math.floor(box.getMinY()); y < Math.ceil(box.getMaxY()); y++) {
        for (int z = (int) Math.floor(box.getMinZ()); z < Math.ceil(box.getMaxZ()); z++) {
          blocks.add(world.getBlockAt(x, y, z));
        }
      }
    }
    return blocks;
  }

  // blocks whose collision shapes could intersect the box; fences, walls, etc. extend into the
  // block above them
  private static List<Block> collisionCandidates(final World world, final BoundingBox box) {
    return blocksIn(world, box.clone().expand(0, 1, 0, 0, 0, 0));
  }

  // check if the block's collision shape intersects the box
  private static boolean collides(final Block block, final BoundingBox box) {
    return block
        .getCollisionShape()
        .overlaps(box.clone().shift(-block.getX(), -block.getY(), -block.getZ()));
  }

  private static boolean isValidFloor(final double floor) {
    return !Double.isInfinite(floor) && floor < 1.0;
  }

  // height above the base of the block a player would stand at, as per vanilla's dismount logic
  private static double floorHeight(final Block feetBlock) {
    // players stand inside scaffolding, on whatever is below it
    if (feetBlock.getType() != Material.SCAFFOLDING) {
      final double top = floorShapeTop(feetBlock);
      if (!Double.isInfinite(top)) {
        return top;
      }
    }
    final double belowTop = floorShapeTop(feetBlock.getRelative(BlockFace.DOWN));
    return belowTop >= 1.0 ? belowTop - 1.0 : Double.NEGATIVE_INFINITY;
  }

  // top of the block's collision shape that can be stood on, or negative infinity if none
  private static double floorShapeTop(final Block block) {
    final Material material = block.getType();
    if ((Tag.CLIMBABLE.isTagged(material) && material != Material.SCAFFOLDING)
        || (block.getBlockData() instanceof final TrapDoor trapDoor && trapDoor.isOpen())) {
      return Double.NEGATIVE_INFINITY;
    }

    double top = Double.NEGATIVE_INFINITY;
    for (final BoundingBox box : block.getCollisionShape().getBoundingBoxes()) {
      top = Math.max(top, box.getMaxY());
    }
    return top;
  }

  // find random safe location within radius
  public static Location getRandomLoc(final Location loc, final int randomRadius) {
    final Random rand = new Random();
    int attempts = 0;
    while (attempts <= randomRadius) {
      attempts++;
      final double angle = rand.nextDouble() * 2 * Math.PI;
      final double distance = rand.nextDouble() * randomRadius;
      final Location location = loc.clone();
      location.add(
          (double) Math.round(distance * Math.cos(angle)),
          0,
          (double) Math.round(distance * Math.sin(angle)));
      if (!isUnsafe(location.getBlock())) {
        return location;
      }
    }
    return loc.clone();
  }

  // play teleport sound effect
  public static void playTeleportSoundNearby(
      final Location fromLoc, final Location toLoc, final Player teleportingPlayer) {
    final Set<Player> notifiedPlayers = new HashSet<>();

    for (final Player player : fromLoc.getNearbyPlayers(SOUND_RADIUS)) {
      if (!player.equals(teleportingPlayer)) {
        player.playSound(
            fromLoc,
            Sound.UI_HUD_BUBBLE_POP,
            (float) (1 - player.getLocation().distance(fromLoc) / SOUND_RADIUS),
            1);
        notifiedPlayers.add(player);
      }
    }

    for (final Player player : toLoc.getNearbyPlayers(SOUND_RADIUS)) {
      if (!player.equals(teleportingPlayer) && !notifiedPlayers.contains(player)) {
        player.playSound(
            toLoc,
            Sound.UI_HUD_BUBBLE_POP,
            (float) (1 - player.getLocation().distance(toLoc) / SOUND_RADIUS),
            1);
      }
    }
  }

  // display teleport particle effects
  private static void showTeleportParticles(final Location loc) {
    final World world = loc.getWorld();
    if (world == null) {
      return;
    }
    final Location bottom = loc.clone();
    final Location top = loc.clone().add(0, 1, 0);

    final Particle.DustTransition dustTransition =
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
