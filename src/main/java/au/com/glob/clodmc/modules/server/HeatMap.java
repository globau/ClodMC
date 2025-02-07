package au.com.glob.clodmc.modules.server;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Logger;
import au.com.glob.clodmc.util.Schedule;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/** Track minutes a chunk is occupied by at least one player */
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
    private final @NotNull PreparedStatement insertStatement;

    DB() {
      try {
        File dbFile = new File(ClodMC.instance.getDataFolder(), "heatmap.sqlite");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
        conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS "
                    + "heatmap(world CHAR, x INT, z INT, count INT, UNIQUE(world, x, z))")
            .execute();
        this.insertStatement =
            conn.prepareStatement(
                "INSERT INTO heatmap(world,x,z,count) "
                    + "VALUES(?,?,?,1) "
                    + "ON CONFLICT(world,x,z) "
                    + "  DO UPDATE SET count=count+1;");
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    void incChunk(@NotNull Chunk chunk) {
      try {
        this.insertStatement.setString(1, chunk.getWorld().getName());
        this.insertStatement.setInt(2, chunk.getX());
        this.insertStatement.setInt(3, chunk.getZ());
        this.insertStatement.execute();
      } catch (SQLException e) {
        Logger.error("failed to update heatmap.sqlite: " + e.getMessage());
      }
    }
  }
}
