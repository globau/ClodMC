package au.com.glob.clodmc.modules.mobs.preventmobspawn;

import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Location;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;

/** cached admin claim bounds for efficient location checking */
@NullMarked
final class AdminClaim {
  private final World world;
  private final int minX;
  private final int minZ;
  private final int maxX;
  private final int maxZ;

  AdminClaim(final Claim claim) {
    // cache claim boundaries for performance; corners are inclusive block coordinates
    this.world = claim.getLesserBoundaryCorner().getWorld();
    this.minX = claim.getLesserBoundaryCorner().getBlockX();
    this.minZ = claim.getLesserBoundaryCorner().getBlockZ();
    this.maxX = claim.getGreaterBoundaryCorner().getBlockX();
    this.maxZ = claim.getGreaterBoundaryCorner().getBlockZ();
  }

  // check if location is within this admin claim
  boolean contains(final Location loc) {
    final int x = loc.getBlockX();
    final int z = loc.getBlockZ();
    return loc.getWorld().equals(this.world)
        && x >= this.minX
        && x <= this.maxX
        && z >= this.minZ
        && z <= this.maxZ;
  }
}
