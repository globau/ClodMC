package au.com.glob.clodmc.modules.server;

// inspired by https://github.com/pop4959/ChunkyBorder/

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.BlueMapModule;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Config;
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
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class CircularWorldBorder implements Module, BlueMapModule, Listener {
  private static final int maxParticleDistance = 8;
  private static final int maxParticleDistanceSquared = maxParticleDistance * maxParticleDistance;
  private static final org.bukkit.Color visualizerColour = org.bukkit.Color.fromRGB(0x20A0FF);

  private final Map<World, Border> borders = new HashMap<>();
  private final Map<UUID, Location> lastPlayerLoc = new HashMap<>();

  public CircularWorldBorder() {
    Config config = ClodMC.instance.getConfig();

    for (World world : Bukkit.getWorlds()) {
      String path = "border." + world.getName();
      if (config.contains(path)) {
        this.borders.put(
            world,
            new Border(
                config.getDouble(path + ".x"),
                config.getDouble(path + ".z"),
                config.getDouble(path + ".radius")));
      }
    }

    this.startBorderCheckTask();
    this.startVisualisationTask();
  }

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
                    TeleportFlag.EntityState.RETAIN_VEHICLE,
                    TeleportFlag.EntityState.RETAIN_PASSENGERS);
              }
            },
            20,
            20);
  }

  private void startVisualisationTask() {
    Particle.DustOptions visualizerOptions = new Particle.DustOptions(visualizerColour, 1);
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

  @Override
  public void onBlueMapEnable() {
    new CircularWorldBorderBlueMap(this);
  }

  protected @NotNull Map<World, Border> getBorders() {
    return this.borders;
  }

  // events

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onCreatureSpawn(CreatureSpawnEvent event) {
    if (this.isOutsideBorder(event.getLocation())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    if (this.isOutsideBorder(event.getBlock().getLocation())) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    this.lastPlayerLoc.remove(event.getPlayer().getUniqueId());
  }

  // border

  private boolean isOutsideBorder(@NotNull Location location) {
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
    if (playerPos.distanceSquared(start) > maxParticleDistanceSquared) {
      return points;
    }
    points.add(start);

    for (double dy = 0; ; ++dy) {
      double up = y + dy;
      double down = y - dy;
      Vector upPos = new Vector(offsetX, up - offsetPercent, offsetZ);
      Vector downPos = new Vector(offsetX, down - offsetPercent, offsetZ);
      boolean upInsideDistance = playerPos.distanceSquared(upPos) <= maxParticleDistanceSquared;
      boolean downInsideDistance = playerPos.distanceSquared(downPos) <= maxParticleDistanceSquared;
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
