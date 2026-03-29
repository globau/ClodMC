package au.com.glob.clodmc.modules.bluemap;

import au.com.glob.clodmc.events.BlueMapInitEvent;
import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jspecify.annotations.NullMarked;

/** displays world spawn points as markers on bluemap */
@NullMarked
public class BlueMapSpawn implements Listener {
  // create spawn point markers for all worlds
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlueMapInit(final BlueMapInitEvent event) {
    final BlueMapAPI api = event.getApi();
    for (final World world : Bukkit.getWorlds()) {
      final MarkerSet markerSet = MarkerSet.builder().label("Spawn").defaultHidden(false).build();
      markerSet.put(
          "spawn",
          POIMarker.builder()
              .label("Spawn")
              .position(
                  Vector3d.from(
                      world.getSpawnLocation().getX() + 0.5,
                      world.getSpawnLocation().getY(),
                      world.getSpawnLocation().getZ() + 0.5))
              .build());

      api.getWorld(world)
          .ifPresent(
              (final BlueMapWorld blueMapWorld) -> {
                for (final BlueMapMap map : blueMapWorld.getMaps()) {
                  map.getMarkerSets().put("spawn", markerSet);
                }
              });
    }
  }
}
