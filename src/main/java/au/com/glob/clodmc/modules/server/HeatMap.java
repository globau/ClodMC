package au.com.glob.clodmc.modules.server;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.bluemap.BlueMap;
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
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import vendored.com.technicjelle.BMUtils.Cheese;

/** Track minutes a chunk is occupied by at least one player */
@NullMarked
public class HeatMap implements Module, Listener {
  public HeatMap() {
    DB db = new DB();

    Schedule.periodically(
        20 * 60,
        () -> {
          Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
          if (onlinePlayers.isEmpty()) {
            return;
          }

          HashSet<Chunk> inhabitedChunks = new HashSet<>(onlinePlayers.size());
          for (Player player : onlinePlayers) {
            inhabitedChunks.add(player.getChunk());
          }

          for (Chunk chunk : inhabitedChunks) {
            db.incChunk(chunk);
          }
        });
  }

  private static class DB {
    private final Connection conn;
    private final PreparedStatement insertStatement;

    DB() {
      try {
        File dbFile = new File(ClodMC.instance.getDataFolder(), "heatmap.sqlite");
        this.conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
        this.conn
            .prepareStatement(
                "CREATE TABLE IF NOT EXISTS "
                    + "heatmap(world CHAR, x INT, z INT, count INT, UNIQUE(world, x, z))")
            .execute();
        this.insertStatement =
            this.conn.prepareStatement(
                "INSERT INTO heatmap(world,x,z,count) "
                    + "VALUES(?,?,?,1) "
                    + "ON CONFLICT(world,x,z) "
                    + "  DO UPDATE SET count=count+1;");
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    void close() {
      try {
        this.conn.close();
      } catch (SQLException e) {
        Logger.error("heatmap.sqlite#close: " + e.getMessage());
      }
    }

    record HeatmapRow(String world, int x, int z, int count) {}

    void incChunk(Chunk chunk) {
      try {
        this.insertStatement.setString(1, chunk.getWorld().getName());
        this.insertStatement.setInt(2, chunk.getX());
        this.insertStatement.setInt(3, chunk.getZ());
        this.insertStatement.execute();
      } catch (SQLException e) {
        Logger.error("heatmap.sqlite#incChunk: " + e.getMessage());
      }
    }

    int getMaxCount(World world) {
      try {
        PreparedStatement s =
            this.conn.prepareStatement("SELECT MAX(`count`) FROM heatmap WHERE world = ?");
        s.setString(1, world.getName());
        ResultSet r = s.executeQuery();
        return r.getInt(1);
      } catch (SQLException e) {
        Logger.error("heatmap.sqlite#getMaxCount: " + e.getMessage());
        return 0;
      }
    }

    int getMarkerCount(World world, int minCount) {
      try {
        PreparedStatement s =
            this.conn.prepareStatement(
                "SELECT COUNT(*) FROM heatmap WHERE world = ? AND count >= ?");
        s.setString(1, world.getName());
        s.setInt(2, minCount);
        ResultSet r = s.executeQuery();
        return r.getInt(1);
      } catch (SQLException e) {
        Logger.error("heatmap.sqlite#getMarkerCount: " + e.getMessage());
        return 0;
      }
    }

    @Nullable Iterator<HeatmapRow> rowIterator(World world, int minCount) {
      try {
        PreparedStatement s =
            this.conn.prepareStatement(
                "SELECT x, z, count FROM heatmap WHERE world = ? AND count >= ?");
        s.setString(1, world.getName());
        s.setInt(2, minCount);
        ResultSet rs = s.executeQuery();
        return new Iterator<>() {
          private boolean hasNext = rs.next();

          @Override
          public boolean hasNext() {
            return this.hasNext;
          }

          @Override
          public HeatmapRow next() {
            if (!this.hasNext) {
              throw new NoSuchElementException();
            }
            try {
              HeatmapRow row =
                  new HeatmapRow(
                      world.getName(), rs.getInt("x"), rs.getInt("z"), rs.getInt("count"));
              this.hasNext = rs.next();
              if (!this.hasNext) {
                rs.close();
                s.close();
              }
              return row;
            } catch (SQLException e) {
              Logger.error("heatmap.sqlite#rowIterator.1: " + e.getMessage());
              throw new NoSuchElementException();
            }
          }
        };
      } catch (SQLException e) {
        Logger.error("heatmap.sqlite#rowIterator.2: " + e.getMessage());
        return null;
      }
    }
  }

  public static class BlueMapHeatMap extends BlueMap.Addon {
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

    private boolean generated = false;

    public BlueMapHeatMap(BlueMapAPI api) {
      super(api);
    }

    @Override
    public void update() {
      if (this.generated) {
        return;
      }
      Schedule.asynchronously(
          () -> {
            this.buildMarkers();
            this.generated = true;
          });
    }

    private void buildMarkers() {
      DB db = new DB();
      try {
        for (World world : Bukkit.getWorlds()) {
          int markerCount = 0;

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

          double maxCount = db.getMaxCount(world);
          if (maxCount == 0) {
            continue;
          }

          // read chunks, grouped by colour
          Iterator<DB.HeatmapRow> iter = db.rowIterator(world, minCount);
          if (iter == null) {
            continue;
          }

          List<List<Vector2i>> chunkLists = new ArrayList<>(COLOURS.length);
          for (int i = 0; i < COLOURS.length; i++) {
            chunkLists.add(new ArrayList<>());
          }

          while (iter.hasNext()) {
            DB.HeatmapRow row = iter.next();
            int index =
                (int)
                    Math.floor(
                        (COLOURS.length - 1) * Math.log(row.count + 1) / Math.log(maxCount + 1));
            chunkLists.get(index).add(new Vector2i(row.x, row.z));
          }

          MarkerSet markerSet = MarkerSet.builder().label("HeatMap").defaultHidden(true).build();

          // overlay between the map and the heatmap markers to make them readable
          WorldBorder border = world.getWorldBorder();
          Location centre = border.getCenter();
          double radius = border.getSize() / 2.0 + 500.0;
          Shape shape =
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
          int id = 0;
          for (int i = 0; i < COLOURS.length; i++) {
            Color colour = COLOURS[i];

            Collection<Cheese> platter =
                Cheese.createPlatterFromChunks(chunkLists.get(i).toArray(new Vector2i[0]));

            // markers
            for (Cheese cheese : platter) {
              markerCount++;
              ShapeMarker marker =
                  ShapeMarker.builder()
                      .shape(cheese.getShape(), world.getMaxHeight() + 2)
                      .label(String.valueOf(chunkLists.get(i).size()))
                      .lineColor(colour)
                      .fillColor(colour)
                      .build();
              markerSet.put("hm" + id++, marker);
            }
          }

          Logger.info("bluemap.heatmap added " + markerCount + " markers to " + world.getName());

          // add to map(s)
          this.api
              .getWorld(world)
              .ifPresent(
                  (BlueMapWorld blueMapWorld) -> {
                    for (BlueMapMap map : blueMapWorld.getMaps()) {
                      map.getMarkerSets().put("hm-" + map.getName(), markerSet);
                    }
                  });
        }
      } finally {
        db.close();
      }
    }
  }
}
