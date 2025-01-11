package au.com.glob.clodmc.modules.bluemap.addon;

import au.com.glob.clodmc.modules.bluemap.BlueMapAddon;
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
import org.jetbrains.annotations.NotNull;

public class WorldBorderAddon extends BlueMapAddon {
  public WorldBorderAddon() {
    super(null);
  }

  @Override
  public void onUpdate(@NotNull BlueMapAPI api) {
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
              .lineColor(new Color(0xA52A2A, 0xFF))
              .fillColor(new Color(0))
              .lineWidth(3)
              .depthTestEnabled(false)
              .build();

      MarkerSet markerSet = MarkerSet.builder().label("World Border").build();
      markerSet.getMarkers().put("ClodMC", marker);
      api.getWorld(world.getName())
          .map(BlueMapWorld::getMaps)
          .ifPresent(
              (Collection<BlueMapMap> maps) ->
                  maps.forEach((BlueMapMap map) -> map.getMarkerSets().put("ClodMC", markerSet)));
    }
  }
}
