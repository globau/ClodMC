package au.com.glob.clodmc.util;

import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class BlockPos {
  public final World world;
  public final int x;
  public final int y;
  public final int z;

  BlockPos(World world, int x, int y, int z) {
    this.world = world;
    this.x = x;
    this.y = y;
    this.z = z;
  }

  @Override
  public String toString() {
    return "BlockPos{" + this.world.getName() + " " + this.x + ", " + this.y + ", " + this.z + '}';
  }

  public String getString(boolean includeWorld) {
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
    return prefix + this.x + ", " + this.y + ", " + this.z;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof BlockPos otherPos)) {
      return false;
    }
    return this.x == otherPos.x
        && this.y == otherPos.y
        && this.z == otherPos.z
        && this.world.equals(otherPos.world);
  }

  public static BlockPos of(Location loc) {
    return new BlockPos(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
  }

  public Location asLocation() {
    return new Location(this.world, this.x + 0.5, this.y, this.z + 0.5);
  }

  public BlockPos down() {
    return new BlockPos(this.world, this.x, this.y - 1, this.z);
  }

  public BlockPos up() {
    return new BlockPos(this.world, this.x, this.y + 1, this.z);
  }

  public Block getBlock() {
    return this.world.getBlockAt(this.x, this.y, this.z);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.world, this.x, this.y, this.z);
  }
}
