package au.com.glob.clodmc.modules.mobs.preventmobspawn;

import au.com.glob.clodmc.annotations.Audience;
import au.com.glob.clodmc.annotations.Doc;
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

@Doc(
    audience = Audience.SERVER,
    title = "Prevent Mob Spawning",
    description = "Prevents enemy mobs from spawning within areas claimed by admin")
@NullMarked
public class PreventMobSpawn implements Listener, Module {
  private final List<AdminClaim> adminClaims = new ArrayList<>(1);

  // cache all admin claims for spawn checking
  @Override
  public void loadConfig() {
    for (Claim claim : GriefPrevention.instance.dataStore.getClaims()) {
      if (claim.isAdminClaim()) {
        this.adminClaims.add(new AdminClaim(claim));
      }
    }
  }

  // prevent enemy mobs from spawning in admin claims
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
