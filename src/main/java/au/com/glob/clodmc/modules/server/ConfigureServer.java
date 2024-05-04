package au.com.glob.clodmc.modules.server;

import au.com.glob.clodmc.ClodMC;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.event.world.WorldInitEvent;

public class ConfigureServer implements Listener {
  public static void register() {
    Bukkit.getServer().getPluginManager().registerEvents(new ConfigureServer(), ClodMC.instance);
  }

  @EventHandler
  public void onWorldInit(WorldInitEvent event) {
    World world = event.getWorld();
    if (world.getEnvironment() == World.Environment.NORMAL) {
      world.setGameRule(GameRule.PLAYERS_SLEEPING_PERCENTAGE, 0);
      world.setGameRule(GameRule.SPAWN_RADIUS, 8);
      world.setSpawnLocation(158, 64, 2);
    }
  }

  @EventHandler
  public void onServerLoadEvent(ServerLoadEvent event) {
    ClodMC.logInfo("clod-mc started");
  }
}
