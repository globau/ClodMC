package au.com.glob.clodmc.modules.server.heapmap;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.util.Logger;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
class DB {
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
          this.conn.prepareStatement("SELECT COUNT(*) FROM heatmap WHERE world = ? AND count >= ?");
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
                new HeatmapRow(world.getName(), rs.getInt("x"), rs.getInt("z"), rs.getInt("count"));
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
