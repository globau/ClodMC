package au.com.glob.clodmc.modules.server;

// inspired by https://github.com/pop4959/ChunkyBorder/

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Logger;
import io.papermc.paper.entity.TeleportFlag;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Implement a circular world border */
public class CircularWorldBorder implements Module, Listener {
  private static final @NotNull List<Config> BORDERS =
      List.of(
          new Config("world", new Border(158.5, 2.5, 7500)),
          new Config("world_nether", new Border(6.5, 0.5, 7500)));

  private static final int MAX_PARTICLE_DISTANCE = 8;
  private static final int MAX_PARTICLE_DISTANCE_SQUARED =
      MAX_PARTICLE_DISTANCE * MAX_PARTICLE_DISTANCE;
  private static final org.bukkit.@NotNull Color VISUALIZER_COLOUR =
      org.bukkit.Color.fromRGB(0x20A0FF);

  private final @NotNull Map<World, Border> borders = new HashMap<>();
  private final @NotNull Map<UUID, Location> lastPlayerLoc = new HashMap<>();

  @Override
  public void loadConfig() {
    for (Config config : BORDERS) {
      World world = Bukkit.getWorld(config.world);
      if (world == null) {
        Logger.error("CircularWorldBorder: invalid world: " + config.world);
        continue;
      }
      this.borders.put(world, config.border);
    }

    this.startBorderCheckTask();
    this.startVisualisationTask();
  }

  private record Config(@NotNull String world, @NotNull Border border) {}

  private void startBorderCheckTask() {
    Bukkit.getServer()
        .getScheduler()
        .scheduleSyncRepeatingTask(
            ClodMC.instance,
            () -> {
              for (Player player : Bukkit.getOnlinePlayers()) {
                Border border = CircularWorldBorder.this.borders.get(player.getWorld());
                if (border == null) {
                  continue;
                }

                Location playerLoc = player.getLocation();

                // player within border, save location
                if (border.isBounding(playerLoc.getX(), playerLoc.getZ())) {
                  CircularWorldBorder.this.lastPlayerLoc.put(player.getUniqueId(), playerLoc);
                  continue;
                }

                // player outside border, teleport to last saved location
                Location teleportLoc =
                    CircularWorldBorder.this.lastPlayerLoc.get(player.getUniqueId());
                teleportLoc =
                    teleportLoc == null
                        ? player.getWorld().getSpawnLocation().clone()
                        : teleportLoc.clone();
                teleportLoc.setPitch(player.getPitch());
                teleportLoc.setYaw(player.getYaw());

                playerLoc.getWorld().playEffect(player.getLocation(), Effect.ENDER_SIGNAL, 0);
                player.playSound(teleportLoc, Sound.ENTITY_PLAYER_TELEPORT, 1, 1);
                player.teleport(
                    teleportLoc,
                    PlayerTeleportEvent.TeleportCause.PLUGIN,
                    TeleportFlag.EntityState.RETAIN_VEHICLE,
                    TeleportFlag.EntityState.RETAIN_PASSENGERS);
              }
            },
            20,
            20);
  }

  private void startVisualisationTask() {
    Particle.DustOptions visualizerOptions = new Particle.DustOptions(VISUALIZER_COLOUR, 1);
    AtomicLong tick = new AtomicLong();
    Bukkit.getServer()
        .getScheduler()
        .scheduleSyncRepeatingTask(
            ClodMC.instance,
            () -> {
              tick.incrementAndGet();
              for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                World world = player.getWorld();
                Border border = this.borders.get(world);
                if (border == null) {
                  return;
                }

                List<Vector> particleLocations =
                    getParticlesAt(player, border, (tick.longValue() % 20) / 20d);

                for (Vector vec : particleLocations) {
                  Location loc = new Location(world, vec.getX(), vec.getY(), vec.getZ());
                  Block block = world.getBlockAt(loc);
                  boolean fullyOccluded =
                      block.getType().isOccluding()
                          && block.getRelative(BlockFace.NORTH).getType().isOccluding()
                          && block.getRelative(BlockFace.EAST).getType().isOccluding()
                          && block.getRelative(BlockFace.SOUTH).getType().isOccluding()
                          && block.getRelative(BlockFace.WEST).getType().isOccluding();
                  if (!fullyOccluded) {
                    player.spawnParticle(Particle.DUST, loc, 1, visualizerOptions);
                  }
                }
              }
            },
            1L,
            1L);
  }

  public @NotNull Map<World, Border> getBorders() {
    return this.borders;
  }

  // events

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onCreatureSpawn(@NotNull CreatureSpawnEvent event) {
    if (this.isOutsideBorder(event.getLocation())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onBlockPlace(@NotNull BlockPlaceEvent event) {
    if (this.isOutsideBorder(event.getBlock().getLocation())) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
    this.lastPlayerLoc.remove(event.getPlayer().getUniqueId());
  }

  // border

  public @Nullable Border getBorder(@NotNull World world) {
    return CircularWorldBorder.this.borders.get(world);
  }

  public boolean isOutsideBorder(@NotNull Location location) {
    Border border = CircularWorldBorder.this.borders.get(location.getWorld());
    return border != null && !border.isBounding(location.getX(), location.getZ());
  }

  public record Border(double x, double z, double r) {
    public Vector2 center() {
      return new Vector2(this.x, this.z);
    }

    public boolean isBounding(double x, double z) {
      return (Math.pow(x - this.x, 2) / Math.pow(this.r, 2))
              + (Math.pow(z - this.z, 2) / Math.pow(this.r, 2))
          <= 1;
    }
  }

  public record Vector2(double x, double z) {}

  private static @NotNull List<Vector> getParticlesAt(
      @NotNull Player player, @NotNull Border border, double offsetPercent) {
    Vector pos = player.getLocation().toVector();
    List<Vector> particles = new ArrayList<>();
    Vector2 center = border.center();
    double angle = Math.acos((2.0 * border.r * border.r - 1) / (2.0 * border.r * border.r));
    double cameraAngle =
        Math.atan2(border.r * (pos.getZ() - center.z), border.r * (pos.getX() - center.x));
    double startY = Math.floor(pos.getY());
    double forwardStartAngle = Math.floor(cameraAngle / angle) * angle;
    double backwardStartAngle = forwardStartAngle - angle;
    double backwardStopAngle = backwardStartAngle - Math.PI;

    for (double da = backwardStartAngle; da > backwardStopAngle; da = da - angle) {
      Vector2 start = pointOnCircle(center.x, center.z, border.r, da);
      Vector2 end = pointOnCircle(center.x, center.z, border.r, da + angle);
      double minX = Math.min(start.x, end.x);
      double minZ = Math.min(start.z, end.z);
      double maxX = Math.max(start.x, end.x);
      double maxZ = Math.max(start.z, end.z);
      Vector startPos = new Vector(start.x, startY, start.z);
      List<Vector> pointsBack =
          verticalPoints(
              pos,
              startPos,
              offsetPercent,
              end.x - start.x,
              end.z - start.z,
              minX,
              minZ,
              maxX,
              maxZ);
      if (pointsBack.isEmpty()) {
        break;
      }
      particles.addAll(pointsBack);
    }

    double forwardStopAngle = forwardStartAngle + Math.PI;
    for (double da = forwardStartAngle; da < forwardStopAngle; da = da + angle) {
      Vector2 start = pointOnCircle(center.x, center.z, border.r, da);
      Vector2 end = pointOnCircle(center.x, center.z, border.r, da + angle);
      double minX = Math.min(start.x, end.x);
      double minZ = Math.min(start.z, end.z);
      double maxX = Math.max(start.x, end.x);
      double maxZ = Math.max(start.z, end.z);
      Vector startPos = new Vector(start.x, startY, start.z);
      List<Vector> pointsBack =
          verticalPoints(
              pos,
              startPos,
              offsetPercent,
              end.x - start.x,
              end.z - start.z,
              minX,
              minZ,
              maxX,
              maxZ);
      if (pointsBack.isEmpty()) {
        break;
      }
      particles.addAll(pointsBack);
    }

    return particles;
  }

  private static @NotNull List<Vector> verticalPoints(
      @NotNull Vector playerPos,
      @NotNull Vector startPos,
      double offsetPercent,
      double unitX,
      double unitZ,
      double minX,
      double minZ,
      double maxX,
      double maxZ) {
    List<Vector> points = new ArrayList<>();

    double x = startPos.getX();
    double y = startPos.getY();
    double z = startPos.getZ();
    double unitOffsetX = unitX * offsetPercent;
    double unitOffsetZ = unitZ * offsetPercent;
    double offsetX = x + unitOffsetX;
    double offsetZ = z + unitOffsetZ;

    if (!(offsetX >= minX && offsetX <= maxX && offsetZ >= minZ && offsetZ <= maxZ)) {
      return points;
    }

    Vector start = new Vector(offsetX, y - offsetPercent, offsetZ);
    if (playerPos.distanceSquared(start) > MAX_PARTICLE_DISTANCE_SQUARED) {
      return points;
    }
    points.add(start);

    for (double dy = 0; ; ++dy) {
      double up = y + dy;
      double down = y - dy;
      Vector upPos = new Vector(offsetX, up - offsetPercent, offsetZ);
      Vector downPos = new Vector(offsetX, down - offsetPercent, offsetZ);
      boolean upInsideDistance = playerPos.distanceSquared(upPos) <= MAX_PARTICLE_DISTANCE_SQUARED;
      boolean downInsideDistance =
          playerPos.distanceSquared(downPos) <= MAX_PARTICLE_DISTANCE_SQUARED;
      if (!upInsideDistance && !downInsideDistance) {
        break;
      }
      if (upInsideDistance) {
        points.add(upPos);
      }
      if (downInsideDistance) {
        points.add(downPos);
      }
    }

    return points;
  }

  private static @NotNull Vector2 pointOnCircle(
      double centerX, double centerZ, double radius, double angle) {
    double x = centerX + radius * Math.cos(angle);
    double z = centerZ + radius * Math.sin(angle);
    return new Vector2(x, z);
  }
}
