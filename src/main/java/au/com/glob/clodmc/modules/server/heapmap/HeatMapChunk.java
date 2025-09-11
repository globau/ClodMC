package au.com.glob.clodmc.modules.server.heapmap;

import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** represents a chunk with its visit count from the heatmap database */
@NullMarked
public final class HeatMapChunk {
  private final String world;
  final int x;
  final int z;
  final int count;

  HeatMapChunk(String world, int x, int z, int count) {
    this.world = world;
    this.x = x;
    this.z = z;
    this.count = count;
  }

  int index(int coloursLength, double maxCount) {
    return (int)
        Math.floor((coloursLength - 1) * Math.log(this.count + 1) / Math.log(maxCount + 1));
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    HeatMapChunk that = (HeatMapChunk) obj;
    return Objects.equals(this.world, that.world) && this.x == that.x && this.z == that.z;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.world, this.x, this.z);
  }
}
