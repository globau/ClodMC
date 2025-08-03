package au.com.glob.clodmc.util;

import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class Vector3i {
  public final int x;
  public final int y;
  public final int z;

  public Vector3i(int x, int y, int z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    Vector3i that = (Vector3i) obj;
    return this.x == that.x && this.y == that.y && this.z == that.z;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.x, this.y, this.z);
  }

  @Override
  public String toString() {
    return "Vector3i[x=%d, y=%d, z=%d]".formatted(this.x, this.y, this.z);
  }
}
