package au.com.glob.clodmc.modules.mobs.preventmobspawn;

import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Location;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;

@NullMarked
final class AdminClaim {
  private final World world;
  private final double minX;
  private final double minZ;
  private final double maxX;
  private final double maxZ;

  AdminClaim(Claim claim) {
    this.world = claim.getLesserBoundaryCorner().getWorld();
    this.minX = claim.getLesserBoundaryCorner().getX();
    this.minZ = claim.getLesserBoundaryCorner().getZ();
    this.maxX = claim.getGreaterBoundaryCorner().getX();
    this.maxZ = claim.getGreaterBoundaryCorner().getZ();
  }

  boolean contains(Location loc) {
    return loc.getWorld().equals(this.world)
        && loc.getX() >= this.minX
        && loc.getX() <= this.maxX
        && loc.getZ() >= this.minZ
        && loc.getZ() <= this.maxZ;
  }
}
