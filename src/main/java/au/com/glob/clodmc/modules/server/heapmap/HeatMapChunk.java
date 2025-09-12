package au.com.glob.clodmc.modules.server.heapmap;

import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** represents a chunk with its visit count from the heatmap database */
@NullMarked
public class HeatMapChunk {
  private final String world;
  int x;
  int z;
  int count;

  HeatMapChunk(final String world, final int x, final int z, final int count) {
    this.world = world;
    this.x = x;
    this.z = z;
    this.count = count;
  }

  int index(final int coloursLength, final double maxCount) {
    return (int)
        Math.floor((coloursLength - 1) * Math.log(this.count + 1) / Math.log(maxCount + 1));
  }

  @Override
  public boolean equals(@Nullable final Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof final HeatMapChunk that)) {
      return false;
    }
    return Objects.equals(this.world, that.world) && this.x == that.x && this.z == that.z;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.world, this.x, this.z);
  }
}
