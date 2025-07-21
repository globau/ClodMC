package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Logger;
import au.com.glob.clodmc.util.StringUtil;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jspecify.annotations.NullMarked;

/** log player death location */
@NullMarked
public class DeathLog implements Module, Listener {
  @EventHandler
  public void onPlayerDeath(PlayerDeathEvent event) {
    String died = StringUtil.asText(event.deathMessage(), event.getPlayer().getName() + " died");
    Location loc = event.getPlayer().getLocation();
    String world =
        loc.getWorld().getName().equals("world") ? "overworld" : loc.getWorld().getName();
    String coords =
        Math.floor(loc.getX()) + "," + Math.floor(loc.getY()) + "," + Math.floor(loc.getZ());
    Logger.info(died + " at " + world + "[" + coords + "]");
  }
}
