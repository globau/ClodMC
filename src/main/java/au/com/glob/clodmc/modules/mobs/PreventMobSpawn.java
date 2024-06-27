package au.com.glob.clodmc.modules.mobs;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.Module;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.entity.Enemy;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;

public class PreventMobSpawn implements Listener, Module {
  private boolean hasGriefPrevention;

  public PreventMobSpawn() {
    try {
      Class.forName("me.ryanhamshire.GriefPrevention.GriefPrevention");
      this.hasGriefPrevention = true;
    } catch (ClassNotFoundException e) {
      ClodMC.logWarning("PreventMobSpawn: GriefPrevention plugin not enabled");
      this.hasGriefPrevention = false;
    }
  }

  @Override
  public boolean forceDisable() {
    return !this.hasGriefPrevention;
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onCreatureSpawnEvent(@NotNull CreatureSpawnEvent event) {
    // prevents enemy mobs from spawning within areas claimed by admin (eg. spawn island)
    Claim claim = GriefPrevention.instance.dataStore.getClaimAt(event.getLocation(), true, null);
    if (claim != null && claim.isAdminClaim() && event.getEntity() instanceof Enemy) {
      event.setCancelled(true);
    }
  }
}
