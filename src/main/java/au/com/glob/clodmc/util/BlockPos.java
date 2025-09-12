package au.com.glob.clodmc.util;

import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** immutable block position with world, x, y, z coordinates */
@NullMarked
public class BlockPos {
  public final World world;
  public final int x;
  public final int y;
  public final int z;

  BlockPos(final World world, final int x, final int y, final int z) {
    this.world = world;
    this.x = x;
    this.y = y;
    this.z = z;
  }

  @Override
  public String toString() {
    return "BlockPos{%s %d, %d, %d}".formatted(this.world.getName(), this.x, this.y, this.z);
  }

  // format position as string with optional world prefix
  public String getString(final boolean includeWorld) {
    String prefix = "";
    if (includeWorld) {
      prefix =
          switch (this.world.getEnvironment()) {
            case NORMAL -> "Overworld ";
            case NETHER -> "Nether ";
            case THE_END -> "The End ";
            default -> "";
          };
    }
    return "%s%d, %d, %d".formatted(prefix, this.x, this.y, this.z);
  }

  @Override
  public boolean equals(@Nullable final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof final BlockPos otherPos)) {
      return false;
    }
    return this.x == otherPos.x
        && this.y == otherPos.y
        && this.z == otherPos.z
        && this.world.equals(otherPos.world);
  }

  // create block position from bukkit location
  public static BlockPos of(final Location loc) {
    return new BlockPos(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
  }

  // convert to bukkit location (centred in final block)
  public Location asLocation() {
    return new Location(this.world, this.x + 0.5, this.y, this.z + 0.5);
  }

  // get position one block below
  public BlockPos down() {
    return new BlockPos(this.world, this.x, this.y - 1, this.z);
  }

  // get position one block above
  public BlockPos up() {
    return new BlockPos(this.world, this.x, this.y + 1, this.z);
  }

  // get the bukkit block at this position
  public Block getBlock() {
    return this.world.getBlockAt(this.x, this.y, this.z);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.world, this.x, this.y, this.z);
  }
}
