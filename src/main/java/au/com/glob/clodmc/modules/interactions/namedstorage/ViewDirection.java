package au.com.glob.clodmc.modules.interactions.namedstorage;

import org.jspecify.annotations.NullMarked;

@NullMarked
record ViewDirection(float yaw, float pitch) {
  float distanceTo(ViewDirection other) {
    return Math.abs(this.yaw - other.yaw) + Math.abs(this.pitch - other.pitch);
  }
}
