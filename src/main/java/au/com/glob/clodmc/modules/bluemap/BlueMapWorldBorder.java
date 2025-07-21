package au.com.glob.clodmc.modules.bluemap;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import java.util.Collection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class BlueMapWorldBorder extends BlueMap.Addon {
  private static final Color LINE_COLOUR = new Color("#a52a2aff");
  private static final Color FILL_COLOUR = new Color("#00000000");

  protected BlueMapWorldBorder(BlueMapAPI api) {
    super(api);
  }

  @Override
  public void update() {
    if (this.api == null) {
      return;
    }

    for (World world : Bukkit.getWorlds()) {
      WorldBorder border = world.getWorldBorder();
      Location centre = border.getCenter();
      double radius = border.getSize() / 2.0;
      Shape shape =
          Shape.createRect(
              centre.getBlockX() - radius,
              centre.getBlockZ() - radius,
              centre.getBlockX() + radius,
              centre.getBlockZ() + radius);
      ShapeMarker marker =
          ShapeMarker.builder()
              .label("World Border")
              .shape(shape, world.getSeaLevel())
              .lineColor(LINE_COLOUR)
              .fillColor(FILL_COLOUR)
              .lineWidth(3)
              .depthTestEnabled(false)
              .build();

      MarkerSet markerSet = MarkerSet.builder().label("World Border").build();
      markerSet.getMarkers().put("ClodMC", marker);
      this.api
          .getWorld(world.getName())
          .map(BlueMapWorld::getMaps)
          .ifPresent(
              (Collection<BlueMapMap> maps) ->
                  maps.forEach((BlueMapMap map) -> map.getMarkerSets().put("ClodMC", markerSet)));
    }
  }
}
