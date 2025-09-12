package au.com.glob.clodmc.util;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.jspecify.annotations.NullMarked;

/** Location related helpers */
@NullMarked
public final class LocationUtil {
  // calculates location the player is facing from given location
  public static Location facingLocation(final Location location) {
    final double yawRadians = Math.toRadians(location.getYaw());
    final double facingX = location.getX() - Math.sin(yawRadians);
    final double facingZ = location.getZ() + Math.cos(yawRadians);
    return new Location(location.getWorld(), facingX, location.getY(), facingZ);
  }

  // gets the block the player is facing from given location
  public static Block facingBlock(final Location location) {
    return facingLocation(location).getBlock();
  }

  // checks if player is facing an air block
  public static boolean isFacingAir(final Location location) {
    return facingBlock(location).isEmpty();
  }

  // checks if player is facing a solid block
  public static boolean isFacingSolid(final Location location) {
    return facingBlock(location).isSolid();
  }
}
