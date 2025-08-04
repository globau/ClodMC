package au.com.glob.clodmc.modules.server.heapmap;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.util.Logger;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;

/** sqlite database for storing heatmap chunk visit counts */
@NullMarked
class DB {
  final Connection conn;
  private final PreparedStatement insertStatement;

  DB() {
    try {
      File dbFile = new File(ClodMC.instance.getDataFolder(), "heatmap.sqlite");
      this.conn = DriverManager.getConnection("jdbc:sqlite:%s".formatted(dbFile));
      this.conn
          .prepareStatement(
              """
              CREATE TABLE IF NOT EXISTS
                heatmap(world CHAR, x INT, z INT, count INT, UNIQUE(world, x, z))
              """)
          .execute();
      this.insertStatement =
          this.conn.prepareStatement(
              """
              INSERT INTO heatmap(world,x,z,count)
              VALUES(?,?,?,1)
              ON CONFLICT(world,x,z)
                DO UPDATE SET count=count+1
              """);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  // close database connection
  void close() {
    try {
      this.conn.close();
    } catch (SQLException e) {
      Logger.error("heatmap.sqlite#close: %s".formatted(e.getMessage()));
    }
  }

  // increment visit count for a chunk
  void incChunk(Chunk chunk) {
    try {
      this.insertStatement.setString(1, chunk.getWorld().getName());
      this.insertStatement.setInt(2, chunk.getX());
      this.insertStatement.setInt(3, chunk.getZ());
      this.insertStatement.execute();
    } catch (SQLException e) {
      Logger.error("heatmap.sqlite#incChunk: %s".formatted(e.getMessage()));
    }
  }

  // get highest visit count for any chunk in world
  int getMaxCount(World world) {
    try (PreparedStatement s =
        this.conn.prepareStatement("SELECT MAX(`count`) FROM heatmap WHERE world = ?")) {
      s.setString(1, world.getName());
      return s.executeQuery().getInt(1);
    } catch (SQLException e) {
      Logger.error("heatmap.sqlite#getMaxCount: %s".formatted(e.getMessage()));
      return 0;
    }
  }

  // count chunks with at least minCount visits
  int getMarkerCount(World world, int minCount) {
    try (PreparedStatement s =
        this.conn.prepareStatement("SELECT COUNT(*) FROM heatmap WHERE world = ? AND count >= ?")) {
      s.setString(1, world.getName());
      s.setInt(2, minCount);
      return s.executeQuery().getInt(1);
    } catch (SQLException e) {
      Logger.error("heatmap.sqlite#getMarkerCount: %s".formatted(e.getMessage()));
      return 0;
    }
  }
}
