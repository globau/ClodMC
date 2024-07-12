package au.com.glob.clodmc.modules.server;

import au.com.glob.clodmc.util.BlueMap;
import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import org.bukkit.Bukkit;
import org.bukkit.World;

public class SpawnMarkerBlueMap {
  public SpawnMarkerBlueMap() {
    assert BlueMap.api != null;

    for (World world : Bukkit.getWorlds()) {
      MarkerSet markerSet = MarkerSet.builder().label("Spawn").defaultHidden(false).build();
      markerSet.put(
          "spawn",
          POIMarker.builder()
              .label("Spawn")
              .position(
                  Vector3d.from(
                      world.getSpawnLocation().getX(),
                      world.getSpawnLocation().getY(),
                      world.getSpawnLocation().getZ()))
              .build());
      BlueMap.api
          .getWorld(world)
          .ifPresent(
              (BlueMapWorld blueMapWorld) -> {
                for (BlueMapMap map : blueMapWorld.getMaps()) {
                  map.getMarkerSets().put("spawn", markerSet);
                }
              });
    }
  }
}
