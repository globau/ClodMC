package au.com.glob.clodmc.modules.mobs.preventmobspawn;

import au.com.glob.clodmc.modules.Module;
import java.util.ArrayList;
import java.util.List;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.entity.Enemy;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jspecify.annotations.NullMarked;

/** Prevents enemy mobs from spawning within areas claimed by admin (eg. spawn island) */
@NullMarked
public class PreventMobSpawn implements Listener, Module {
  private final List<AdminClaim> adminClaims = new ArrayList<>(1);

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

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onCreatureSpawn(CreatureSpawnEvent event) {
    if (event.getEntity() instanceof Enemy) {
      for (AdminClaim adminClaim : this.adminClaims) {
        if (adminClaim.contains(event.getLocation())) {
          event.setCancelled(true);
          return;
        }
      }
    }
  }
}
