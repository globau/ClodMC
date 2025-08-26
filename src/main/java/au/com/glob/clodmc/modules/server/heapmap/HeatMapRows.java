package au.com.glob.clodmc.modules.server.heapmap;

import au.com.glob.clodmc.util.Logger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;

/** iterable database result set for heatmap rows with resource management */
@NullMarked
public class HeatMapRows implements Iterable<HeatMapRow>, AutoCloseable {
  private final PreparedStatement statement;
  private final ResultSet resultSet;

  HeatMapRows(DB db, World world, int minCount) throws SQLException {
    this.statement =
        db.conn.prepareStatement(
            "SELECT world, x, z, count FROM heatmap WHERE world = ? AND count >= ?");
    this.statement.setString(1, world.getName());
    this.statement.setInt(2, minCount);
    this.resultSet = this.statement.executeQuery();
  }

  // create iterator over database results
  @Override
  public Iterator<HeatMapRow> iterator() {
    try {
      return new Iterator<>() {
        private boolean hasNext = HeatMapRows.this.resultSet.next();

        @Override
        public boolean hasNext() {
          return this.hasNext;
        }

        @Override
        public HeatMapRow next() {
          if (!this.hasNext) {
            throw new NoSuchElementException();
          }
          try {
            HeatMapRow row =
                new HeatMapRow(
                    HeatMapRows.this.resultSet.getString("world"),
                    HeatMapRows.this.resultSet.getInt("x"),
                    HeatMapRows.this.resultSet.getInt("z"),
                    HeatMapRows.this.resultSet.getInt("count"));
            this.hasNext = HeatMapRows.this.resultSet.next();
            return row;
          } catch (SQLException e) {
            throw new RuntimeException(e);
          }
        }
      };
    } catch (SQLException e) {
      Logger.exception(e);
      throw new NoSuchElementException();
    }
  }

  // close database resources
  @Override
  public void close() throws SQLException {
    this.resultSet.close();
    this.statement.close();
  }
}
