package au.com.glob.clodmc.util;

import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Same as Location, but for the block */
public class BlockPos {
  final @NotNull World world;
  final int x;
  final int y;
  final int z;

  private BlockPos(@NotNull World world, int x, int y, int z) {
    this.world = world;
    this.x = x;
    this.y = y;
    this.z = z;
  }

  @Override
  public @NotNull String toString() {
    return "BlockPos{" + this.x + ", " + this.y + ", " + this.z + '}';
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || this.getClass() != other.getClass()) {
      return false;
    }
    BlockPos otherPos = (BlockPos) other;
    return this.x == otherPos.x
        && this.y == otherPos.y
        && this.z == otherPos.z
        && this.world.equals(otherPos.world);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.world, this.x, this.y, this.z);
  }

  public static BlockPos of(@NotNull Location loc) {
    return new BlockPos(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
  }

  public static BlockPos of(@NotNull Location loc, int deltaX, int deltaY, int deltaZ) {
    return new BlockPos(
        loc.getWorld(),
        loc.getBlockX() + deltaX,
        loc.getBlockY() + deltaY,
        loc.getBlockZ() + deltaZ);
  }

  public @NotNull Location asLocation() {
    return new Location(this.world, this.x + 0.5, this.y, this.z + 0.5);
  }

  public @NotNull World getWorld() {
    return this.world;
  }

  public int getX() {
    return this.x;
  }

  public int getY() {
    return this.y;
  }

  public int getZ() {
    return this.z;
  }

  public @NotNull BlockPos down() {
    return new BlockPos(this.world, this.x, this.y - 1, this.z);
  }

  public @NotNull BlockPos up() {
    return new BlockPos(this.world, this.x, this.y + 1, this.z);
  }

  public @NotNull Block getBlock() {
    return this.world.getBlockAt(this.x, this.y, this.z);
  }
}
