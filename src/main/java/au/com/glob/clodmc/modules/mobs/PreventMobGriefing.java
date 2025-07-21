package au.com.glob.clodmc.modules.mobs;

import au.com.glob.clodmc.modules.Module;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Enderman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.jspecify.annotations.NullMarked;

/** Prevent some mobs from breaking or moving blocks */
@NullMarked
public class PreventMobGriefing implements Listener, Module {
  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onEntityExplode(EntityExplodeEvent event) {
    // stop creeper explosions from destroying blocks
    if (event.getEntity() instanceof Creeper) {
      event.blockList().clear();
    }
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onEntityChangeBlock(EntityChangeBlockEvent event) {
    // stop endermen from picking up blocks
    if (event.getEntity() instanceof Enderman) {
      event.setCancelled(true);
    }
  }
}
