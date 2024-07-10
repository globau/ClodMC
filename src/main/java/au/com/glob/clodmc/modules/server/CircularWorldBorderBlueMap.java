package au.com.glob.clodmc.modules.server;

// inspired by https://github.com/pop4959/ChunkyBorder/

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import java.util.Collection;
import java.util.Map;
import org.bukkit.World;

public class CircularWorldBorderBlueMap {
  private static final int mapColour = 0xA52A2A;

  public static void register(Map<World, CircularWorldBorder.Border> borders) {
    BlueMapAPI.onEnable(
        (BlueMapAPI blueMapAPI) -> {
          for (Map.Entry<World, CircularWorldBorder.Border> entry : borders.entrySet()) {
            World world = entry.getKey();
            CircularWorldBorder.Border border = entry.getValue();

            Shape shape = Shape.createCircle(border.x(), border.z(), border.r(), 100);
            ShapeMarker marker =
                ShapeMarker.builder()
                    .label("World Border")
                    .shape(shape, world.getSeaLevel())
                    .lineColor(new Color(CircularWorldBorderBlueMap.mapColour, 0xFF))
                    .fillColor(new Color(0))
                    .lineWidth(3)
                    .depthTestEnabled(false)
                    .build();

            MarkerSet markerSet = MarkerSet.builder().label("World Border").build();
            markerSet.getMarkers().put("ClodMC", marker);
            blueMapAPI
                .getWorld(world.getName())
                .map(BlueMapWorld::getMaps)
                .ifPresent(
                    (Collection<BlueMapMap> maps) ->
                        maps.forEach(
                            (BlueMapMap map) -> map.getMarkerSets().put("ClodMC", markerSet)));
          }
        });
  }
}
