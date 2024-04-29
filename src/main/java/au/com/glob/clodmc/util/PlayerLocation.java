package au.com.glob.clodmc.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@SerializableAs("Location")
public class PlayerLocation implements ConfigurationSerializable {
  private static final int RADIUS = 3;
  private static final Vector3D[] VOLUME;
  private static final Set<Material> DAMAGING_TYPES =
      getMatchingMaterials(
          "CACTUS",
          "CAMPFIRE",
          "FIRE",
          "MAGMA_BLOCK",
          "SOUL_CAMPFIRE",
          "SOUL_FIRE",
          "SWEET_BERRY_BUSH",
          "WITHER_ROSE");
  private static final Set<Material> LAVA_TYPES =
      getMatchingMaterials("FLOWING_LAVA", "LAVA", "STATIONARY_LAVA");
  private static final Material PORTAL = getMaterial("NETHER_PORTAL", "PORTAL");
  private static final Material LIGHT = getMaterial("LIGHT");
  private static final Material PATH = getMaterial("GRASS_PATH");
  private static final Material FARMLAND = getMaterial("FARMLAND");
  private static final Set<Material> BEDS;

  // The player can stand inside these materials
  private static final Set<Material> HOLLOW_MATERIALS = EnumSet.noneOf(Material.class);

  static {
    for (final Material mat : Material.values()) {
      if (!mat.isOccluding()) {
        HOLLOW_MATERIALS.add(mat);
      }
    }
    HOLLOW_MATERIALS.remove(Material.BARRIER);
    HOLLOW_MATERIALS.remove(PATH);
    HOLLOW_MATERIALS.remove(FARMLAND);
    if (LIGHT != null) {
      HOLLOW_MATERIALS.add(LIGHT);
    }

    BEDS =
        getMatchingMaterials(
            "BED",
            "BED_BLOCK",
            "WHITE_BED",
            "ORANGE_BED",
            "MAGENTA_BED",
            "LIGHT_BLUE_BED",
            "YELLOW_BED",
            "LIME_BED",
            "PINK_BED",
            "GRAY_BED",
            "LIGHT_GRAY_BED",
            "CYAN_BED",
            "PURPLE_BED",
            "BLUE_BED",
            "BROWN_BED",
            "GREEN_BED",
            "RED_BED",
            "BLACK_BED");
  }

  static {
    final List<Vector3D> pos = new ArrayList<>();
    for (int x = -RADIUS; x <= RADIUS; x++) {
      for (int y = -RADIUS; y <= RADIUS; y++) {
        for (int z = -RADIUS; z <= RADIUS; z++) {
          pos.add(new Vector3D(x, y, z));
        }
      }
    }
    pos.sort(Comparator.comparingInt(a -> a.x * a.x + a.y * a.y + a.z * a.z));
    VOLUME = pos.toArray(new Vector3D[0]);
  }

  private final Location location;
  private final boolean isFlying;

  private PlayerLocation(@NotNull Location location, boolean isFlying) {
    this.location = location;
    this.isFlying = isFlying;
  }

  public static @NotNull PlayerLocation of(@NotNull Player player) {
    return new PlayerLocation(player.getLocation(), player.isFlying());
  }

  public static @NotNull PlayerLocation of(@NotNull Location location) {
    return new PlayerLocation(location, false);
  }

  public void teleportPlayer(@NotNull Player player) throws LocationError {
    Location loc;
    if (player.isInsideVehicle()) {
      if (player.getVehicle() instanceof Horse) {
        player.sendRichMessage("<grey>Dismounting your horse</grey>");
      } else if (player.getVehicle() instanceof Boat) {
        player.sendRichMessage("<grey>Dismounting your boat</grey>");
      } else {
        player.sendRichMessage("<grey>Dismounting</grey>");
      }
    }
    if (player.getGameMode().equals(GameMode.CREATIVE)) {
      player.setFlying(this.isFlying);
      loc = this.location;
    } else {
      loc = getSafeDestination(this.location);
    }
    player.teleportAsync(loc);
  }

  @Override
  public @NotNull Map<String, Object> serialize() {
    Map<String, Object> serialised = this.location.serialize();
    serialised.put("isFlying", this.isFlying);
    return serialised;
  }

  @SuppressWarnings("unused")
  public static @NotNull PlayerLocation deserialize(@NotNull Map<String, Object> args) {
    return new PlayerLocation(Location.deserialize(args), (Boolean) args.get("isFlying"));
  }

  public static class LocationError extends Exception {
    public LocationError(String message) {
      super(message);
    }
  }

  public static Location getSafeDestination(final Location loc) throws LocationError {
    if (loc == null || loc.getWorld() == null) {
      throw new LocationError("destination not set");
    }
    final World world = loc.getWorld();
    final int worldMinY = world.getMinHeight();
    final int worldLogicalY = world.getLogicalHeight();
    final int worldMaxY = loc.getBlockY() < worldLogicalY ? worldLogicalY : world.getMaxHeight();
    int x = loc.getBlockX();
    int y = (int) Math.round(loc.getY());
    int z = loc.getBlockZ();
    if (isBlockOutsideWorldBorder(world, x, z)) {
      x = getXInsideWorldBorder(world, x);
      z = getZInsideWorldBorder(world, z);
    }
    final int origX = x;
    final int origY = y;
    final int origZ = z;
    while (isBlockAboveAir(world, x, y, z)) {
      y -= 1;
      if (y < 0) {
        y = origY;
        break;
      }
    }
    if (isBlockUnsafe(world, x, y, z)) {
      x = Math.round(loc.getX()) == origX ? x - 1 : x + 1;
      z = Math.round(loc.getZ()) == origZ ? z - 1 : z + 1;
    }
    int i = 0;
    while (isBlockUnsafe(world, x, y, z)) {
      i++;
      if (i >= VOLUME.length) {
        x = origX;
        y = constrainToRange(origY + RADIUS, worldMinY, worldMaxY);
        z = origZ;
        break;
      }
      x = origX + VOLUME[i].x;
      y = constrainToRange(origY + VOLUME[i].y, worldMinY, worldMaxY);
      z = origZ + VOLUME[i].z;
    }
    while (isBlockUnsafe(world, x, y, z)) {
      y += 1;
      if (y >= worldMaxY) {
        x += 1;
        break;
      }
    }
    while (isBlockUnsafe(world, x, y, z)) {
      y -= 1;
      if (y <= worldMinY + 1) {
        x += 1;
        // Allow spawning at the top of the world, but not above the nether roof
        y = Math.min(world.getHighestBlockYAt(x, z) + 1, worldMaxY);
        if (x - 48 > loc.getBlockX()) {
          throw new LocationError("Hole in floor!");
        }
      }
    }
    return new Location(world, x + 0.5, y, z + 0.5, loc.getYaw(), loc.getPitch());
  }

  private static boolean isBlockOutsideWorldBorder(final World world, final int x, final int z) {
    final Location center = world.getWorldBorder().getCenter();
    final int radius = (int) world.getWorldBorder().getSize() / 2;
    final int x1 = center.getBlockX() - radius;
    final int x2 = center.getBlockX() + radius;
    final int z1 = center.getBlockZ() - radius;
    final int z2 = center.getBlockZ() + radius;
    return x < x1 || x > x2 || z < z1 || z > z2;
  }

  private static int getXInsideWorldBorder(final World world, final int x) {
    final Location center = world.getWorldBorder().getCenter();
    final int radius = (int) world.getWorldBorder().getSize() / 2;
    final int x1 = center.getBlockX() - radius;
    final int x2 = center.getBlockX() + radius;
    if (x < x1) {
      return x1;
    } else if (x > x2) {
      return x2;
    }
    return x;
  }

  private static int getZInsideWorldBorder(final World world, final int z) {
    final Location center = world.getWorldBorder().getCenter();
    final int radius = (int) world.getWorldBorder().getSize() / 2;
    final int z1 = center.getBlockZ() - radius;
    final int z2 = center.getBlockZ() + radius;
    if (z < z1) {
      return z1;
    } else if (z > z2) {
      return z2;
    }
    return z;
  }

  private static boolean isBlockUnsafe(final World world, final int x, final int y, final int z) {
    return isBlockDamaging(world, x, y, z) || isBlockAboveAir(world, x, y, z);
  }

  private static boolean isBlockDamaging(final World world, final int x, final int y, final int z) {
    final Material block = world.getBlockAt(x, y, z).getType();
    final Material below = world.getBlockAt(x, y - 1, z).getType();
    final Material above = world.getBlockAt(x, y + 1, z).getType();

    if (DAMAGING_TYPES.contains(below) || LAVA_TYPES.contains(below) || isBed(below)) {
      return true;
    }

    if (block == PORTAL) {
      return true;
    }

    return !HOLLOW_MATERIALS.contains(block) || !HOLLOW_MATERIALS.contains(above);
  }

  private static boolean isBlockAboveAir(final World world, final int x, final int y, final int z) {
    return y > world.getMaxHeight()
        || HOLLOW_MATERIALS.contains(world.getBlockAt(x, y - 1, z).getType());
  }

  private record Vector3D(int x, int y, int z) {}

  private static <T extends Enum<T>> Set<T> getMatchingMaterials(final String... names) {
    //noinspection unchecked
    final Set<T> set = EnumSet.noneOf((Class<T>) Material.class);

    for (final String name : names) {
      try {
        final Field enumField = Material.class.getDeclaredField(name);

        if (enumField.isEnumConstant()) {
          //noinspection unchecked
          set.add((T) enumField.get(null));
        }
      } catch (final NoSuchFieldException | IllegalAccessException e) {
        // ignored
      }
    }
    return set;
  }

  private static <T extends Enum<T>> T valueOfMaterial(final String... names) {
    for (final String name : names) {
      try {
        final Field enumField = Material.class.getDeclaredField(name);

        if (enumField.isEnumConstant()) {
          //noinspection unchecked
          return (T) enumField.get(null);
        }
      } catch (final NoSuchFieldException | IllegalAccessException e) {
        // ignored
      }
    }

    return null;
  }

  private static Material getMaterial(final String... names) {
    return valueOfMaterial(names);
  }

  private static boolean isBed(final Material material) {
    return BEDS.contains(material);
  }

  private static int constrainToRange(int value, int min, int max) {
    return Math.min(Math.max(value, min), max);
  }
}
