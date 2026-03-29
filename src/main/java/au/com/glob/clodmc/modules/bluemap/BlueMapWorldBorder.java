package au.com.glob.clodmc.modules.bluemap;

import au.com.glob.clodmc.events.BlueMapInitEvent;
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
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jspecify.annotations.NullMarked;

/** displays world borders as markers on bluemap */
@NullMarked
public class BlueMapWorldBorder implements Listener {
  private static final Color LINE_COLOUR = new Color("#a52a2aff");
  private static final Color FILL_COLOUR = new Color("#00000000");

  // create world border markers for all worlds
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlueMapInit(final BlueMapInitEvent event) {
    final BlueMapAPI api = event.getApi();

    for (final World world : Bukkit.getWorlds()) {
      final WorldBorder border = world.getWorldBorder();
      final Location centre = border.getCenter();
      final double radius = border.getSize() / 2.0;
      final Shape shape =
          Shape.createRect(
              centre.getBlockX() - radius,
              centre.getBlockZ() - radius,
              centre.getBlockX() + radius,
              centre.getBlockZ() + radius);
      final ShapeMarker marker =
          ShapeMarker.builder()
              .label("World Border")
              .shape(shape, world.getSeaLevel())
              .lineColor(LINE_COLOUR)
              .fillColor(FILL_COLOUR)
              .lineWidth(3)
              .depthTestEnabled(false)
              .build();

      final MarkerSet markerSet = MarkerSet.builder().label("World Border").build();
      markerSet.getMarkers().put("ClodMC", marker);
      api.getWorld(world.getName())
          .map(BlueMapWorld::getMaps)
          .ifPresent(
              (Collection<BlueMapMap> maps) ->
                  maps.forEach((BlueMapMap map) -> map.getMarkerSets().put("ClodMC", markerSet)));
    }
  }
}
