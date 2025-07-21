package au.com.glob.clodmc.modules.bluemap;

import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class BlueMapSpawn extends BlueMap.Addon {
  protected BlueMapSpawn(BlueMapAPI api) {
    super(api);
  }

  @Override
  public void update() {
    if (this.api == null) {
      return;
    }

    for (World world : Bukkit.getWorlds()) {
      MarkerSet markerSet = MarkerSet.builder().label("Spawn").defaultHidden(false).build();
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
      this.api
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
