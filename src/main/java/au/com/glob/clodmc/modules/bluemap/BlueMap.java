package au.com.glob.clodmc.modules.bluemap;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.annotations.Audience;
import au.com.glob.clodmc.annotations.Doc;
import au.com.glob.clodmc.events.BlueMapInitEvent;
import au.com.glob.clodmc.events.ModuleInitialiseEvent;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.server.heapmap.BlueMapHeatMap;
import au.com.glob.clodmc.util.Logger;
import au.com.glob.clodmc.util.Schedule;
import de.bluecolored.bluemap.api.BlueMapAPI;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jspecify.annotations.NullMarked;

@Doc(
    audience = Audience.SERVER,
    title = "BlueMap Integration",
    description = "Bridge between ClodMC modules and BlueMap")
@NullMarked
public class BlueMap extends Module implements Listener {
  public BlueMap() {
    super("BlueMap");
  }

  // initialise all bluemap addons when api is available
  @EventHandler
  public void onModuleInitialise(final ModuleInitialiseEvent event) {
    BlueMapAPI.onEnable(
        (final BlueMapAPI api) -> {
          Logger.info("Initialising BlueMap addons");

          ClodMC.registerListener(new BlueMapSpawn());
          ClodMC.registerListener(new BlueMapWorldBorder());
          ClodMC.registerListener(new BlueMapHeatMap());
          if (ClodMC.isPluginEnabled("GriefPrevention")) {
            ClodMC.registerListener(new BlueMapGriefPrevention());
          }

          // delayed to avoid issue where bluemap was ignoring some addons
          Schedule.delayed(
              5 * 20,
              () -> {
                Logger.info("Triggering BlueMap addon updates");
                Bukkit.getPluginManager().callEvent(new BlueMapInitEvent(api));
              });
        });
  }
}
