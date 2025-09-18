package au.com.glob.clodmc.events;

import org.bukkit.World;
import org.jspecify.annotations.NullMarked;

@NullMarked
record ViewDirection(World world, float yaw, float pitch) {
  float rotationTo(final ViewDirection other) {
    return Math.abs(this.yaw - other.yaw) + Math.abs(this.pitch - other.pitch);
  }
}
