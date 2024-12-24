package au.com.glob.clodmc.modules.mobs;

import au.com.glob.clodmc.modules.Module;
import java.util.ArrayList;
import java.util.List;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Enemy;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;

/** Prevents enemy mobs from spawning within areas claimed by admin (eg. spawn island) */
public class PreventMobSpawn implements Listener, Module {
  private final @NotNull List<AdminClaim> adminClaims = new ArrayList<>(1);

  @Override
  public void initialise() {
    for (Claim claim : GriefPrevention.instance.dataStore.getClaims()) {
      if (claim.isAdminClaim()) {
        this.adminClaims.add(new AdminClaim(claim));
      }
    }
  }

  @Override
  public String dependsOn() {
    return "GriefPrevention";
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onCreatureSpawnEvent(@NotNull CreatureSpawnEvent event) {
    if (event.getEntity() instanceof Enemy) {
      for (AdminClaim adminClaim : this.adminClaims) {
        if (adminClaim.contains(event.getLocation())) {
          event.setCancelled(true);
          return;
        }
      }
    }
  }

  private static final class AdminClaim {
    private final @NotNull World world;
    private final double minX;
    private final double minZ;
    private final double maxX;
    private final double maxZ;

    private AdminClaim(@NotNull Claim claim) {
      this.world = claim.getLesserBoundaryCorner().getWorld();
      this.minX = claim.getLesserBoundaryCorner().getX();
      this.minZ = claim.getLesserBoundaryCorner().getZ();
      this.maxX = claim.getGreaterBoundaryCorner().getX();
      this.maxZ = claim.getGreaterBoundaryCorner().getZ();
    }

    boolean contains(@NotNull Location loc) {
      return loc.getWorld().equals(this.world)
          && loc.getX() >= this.minX
          && loc.getX() <= this.maxX
          && loc.getZ() >= this.minZ
          && loc.getZ() <= this.maxZ;
    }
  }
}
