package au.com.glob.clodmc.modules.server.heapmap;

import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class HeatmapRow {
  private final String world;
  final int x;
  final int z;
  final int count;

  HeatmapRow(String world, int x, int z, int count) {
    this.world = world;
    this.x = x;
    this.z = z;
    this.count = count;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    HeatmapRow that = (HeatmapRow) obj;
    return Objects.equals(this.world, that.world) && this.x == that.x && this.z == that.z;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.world, this.x, this.z);
  }
}
