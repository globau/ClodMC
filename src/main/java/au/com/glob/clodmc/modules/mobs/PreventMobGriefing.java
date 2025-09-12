package au.com.glob.clodmc.modules.mobs;

import au.com.glob.clodmc.annotations.Audience;
import au.com.glob.clodmc.annotations.Doc;
import au.com.glob.clodmc.modules.Module;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Enderman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.jspecify.annotations.NullMarked;

@Doc(
    audience = Audience.SERVER,
    title = "Prevent Mob Griefing",
    description = "Prevent some mobs from breaking or moving blocks")
@NullMarked
public class PreventMobGriefing implements Listener, Module {
  // prevent creeper explosions from destroying blocks
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onEntityExplode(final EntityExplodeEvent event) {
    // stop creeper explosions from destroying blocks
    if (event.getEntity() instanceof Creeper) {
      event.blockList().clear();
    }
  }

  // prevent endermen from picking up blocks
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onEntityChangeBlock(final EntityChangeBlockEvent event) {
    // stop endermen from picking up blocks
    if (event.getEntity() instanceof Enderman) {
      event.setCancelled(true);
    }
  }
}
