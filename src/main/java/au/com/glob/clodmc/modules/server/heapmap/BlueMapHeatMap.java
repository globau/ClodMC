package au.com.glob.clodmc.modules.server.heapmap;

import au.com.glob.clodmc.events.BlueMapInitEvent;
import au.com.glob.clodmc.util.Logger;
import au.com.glob.clodmc.util.Schedule;
import com.flowpowered.math.vector.Vector2i;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import vendored.com.technicjelle.BMUtils.Cheese;

/** generates bluemap heatmap markers from player activity data */
@NullMarked
public class BlueMapHeatMap implements Listener {
  private volatile @Nullable BlueMapAPI api;

  private static final Color[] COLOURS = {
    // viridis heatmap colours (hottest last)
    new Color("#440154"),
    new Color("#481a6c"),
    new Color("#472f7d"),
    new Color("#414487"),
    new Color("#39568c"),
    new Color("#31688e"),
    new Color("#2a788e"),
    new Color("#23888e"),
    new Color("#1f988b"),
    new Color("#22a884"),
    new Color("#35b779"),
    new Color("#54c568"),
    new Color("#7ad151"),
    new Color("#a5db36"),
    new Color("#d2e21b"),
    new Color("#fde725")
  };
  private static final Color OVERLAY_COLOUR = new Color("#000000aa");

  private static final int MAX_MARKERS_PER_WORLD = 750;

  // generate heatmap markers once
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlueMapInit(final BlueMapInitEvent event) {
    this.api = event.getApi();
    Schedule.asynchronously(this::buildMarkers);
  }

  // build heatmap markers for all worlds
  private void buildMarkers() {
    final BlueMapAPI api = this.api;
    if (api == null) {
      return;
    }

    try (final DB db = new DB()) {
      for (final World world : Bukkit.getWorlds()) {
        // determine min and max heatmap values
        int minCount = 0;
        int rowCount;
        while (true) {
          rowCount = db.getMarkerCount(world, minCount);
          if (rowCount <= MAX_MARKERS_PER_WORLD) {
            break;
          }
          minCount++;
        }

        final double maxCount = db.getMaxCount(world);
        if (maxCount == 0) {
          continue;
        }

        final List<List<Vector2i>> chunkLists = new ArrayList<>(COLOURS.length);
        for (int i = 0; i < COLOURS.length; i++) {
          chunkLists.add(new ArrayList<>());
        }

        // read chunks, grouped by colour
        try {
          for (final HeatMapChunk chunk : db.getChunks(world, minCount)) {
            final int index = chunk.index(COLOURS.length, maxCount);
            chunkLists.get(index).add(new Vector2i(chunk.x, chunk.z));
          }
        } catch (final SQLException e) {
          Logger.exception(e);
          continue;
        }

        final MarkerSet markerSet =
            MarkerSet.builder().label("HeatMap").defaultHidden(true).build();

        // overlay between the map and the heatmap markers to make them readable

        final WorldBorder border = world.getWorldBorder();
        final Location centre = border.getCenter();
        final double radius = border.getSize() / 2.0 + 500.0;
        final Shape shape =
            Shape.createRect(
                centre.getBlockX() - radius,
                centre.getBlockZ() - radius,
                centre.getBlockX() + radius,
                centre.getBlockZ() + radius);

        markerSet.put(
            "heatmap-overlay",
            ShapeMarker.builder()
                .shape(shape, world.getMaxHeight() + 1)
                .lineColor(OVERLAY_COLOUR)
                .fillColor(OVERLAY_COLOUR)
                .label("")
                .build());

        // create platter and marker for each colour
        int markerCount = 0;
        int id = 0;
        for (int i = 0; i < COLOURS.length; i++) {
          final Color colour = COLOURS[i];

          final Collection<Cheese> platter =
              Cheese.createPlatterFromChunks(chunkLists.get(i).toArray(new Vector2i[0]));

          // markers
          for (final Cheese cheese : platter) {
            markerCount++;
            final ShapeMarker marker =
                ShapeMarker.builder()
                    .shape(cheese.getShape(), world.getMaxHeight() + 2)
                    .label("")
                    .lineColor(colour)
                    .fillColor(colour)
                    .build();
            markerSet.put("hm%d".formatted(id++), marker);
          }
        }

        Logger.info(
            "bluemap.heatmap added %d markers to %s".formatted(markerCount, world.getName()));

        // add to map(s)
        api.getWorld(world)
            .ifPresent(
                (final BlueMapWorld blueMapWorld) -> {
                  for (final BlueMapMap map : blueMapWorld.getMaps()) {
                    map.getMarkerSets().put("hm-%s".formatted(map.getName()), markerSet);
                  }
                });
      }
    }
  }
}
