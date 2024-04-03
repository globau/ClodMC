package au.com.glob.clodmc.modules.spawn;

import au.com.glob.clodmc.ClodMC;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Bukkit;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;

public class PreventMobSpawn implements Listener {
  public static void register() {
    Bukkit.getServer().getPluginManager().registerEvents(new PreventMobSpawn(), ClodMC.instance);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onCreatureSpawnEvent(@NotNull CreatureSpawnEvent event) {
    // prevents enemy mobs from spawning within areas claimed by admin (eg. spawn island)
    Claim claim = GriefPrevention.instance.dataStore.getClaimAt(event.getLocation(), true, null);
    if (claim != null && claim.isAdminClaim() && event.getEntity() instanceof Monster) {
      event.setCancelled(true);
    }
  }
}
