package au.com.glob.clodmc.util;

import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** immutable 3d integer vector for coordinates */
@NullMarked
public class Vector3i {
  public int x;
  public int y;
  public int z;

  // create vector with x, y, z coordinates
  public Vector3i(final int x, final int y, final int z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  // check equality based on coordinates
  @Override
  public boolean equals(@Nullable final Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof final Vector3i that)) {
      return false;
    }
    return this.x == that.x && this.y == that.y && this.z == that.z;
  }

  // generate hash code from coordinates
  @Override
  public int hashCode() {
    return Objects.hash(this.x, this.y, this.z);
  }

  // format as vector string representation
  @Override
  public String toString() {
    return "Vector3i[x=%d, y=%d, z=%d]".formatted(this.x, this.y, this.z);
  }
}
