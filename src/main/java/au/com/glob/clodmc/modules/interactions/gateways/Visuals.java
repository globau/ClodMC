package au.com.glob.clodmc.modules.interactions.gateways;

import au.com.glob.clodmc.util.Schedule;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** manages visual effects and particles for gateway anchors */
@NullMarked
public class Visuals {
  private static final int VISIBLE_RANGE_SQUARED = 32 * 32;
  private static final int ACTIVE_RING_COUNT = 8;
  private static final int INACTIVE_RING_COUNT = 4;
  private static final double EFFECT_RADIUS = 0.375;
  private static final double EFFECT_SPEED_ACTIVE = 0.075;
  private static final double EFFECT_SPEED_INACTIVE = 0.05;
  private static final int EFFECT_PARTICLES = 4;
  private static final int EFFECT_PARTICLES_TRAIL_ACTIVE = 5;
  private static final int EFFECT_PARTICLES_TRAIL_INACTIVE = 3;
  private static final double ANGLE_STEP = 2 * Math.PI / EFFECT_PARTICLES;

  private final AnchorBlock anchorBlock;
  private final Location blockLocation;
  private final Location topLocation;
  private final Location bottomLocation;
  final Colour topColour;
  final Colour bottomColour;

  private @Nullable BukkitTask particleTask = null;
  private final List<NearbyPlayer> nearbyPlayers = new ArrayList<>();

  private final double[] activeBottomRingY;
  private final double[] activeTopRingY;
  private final Color[] activeBottomRingColours;
  private final Color[] activeTopRingColours;

  private final double[] inactiveBottomRingY;
  private final double[] inactiveTopRingY;
  private final Color[] inactiveBottomRingColours;
  private final Color[] inactiveTopRingColours;

  private final double[] ringRotationOffsets;

  Visuals(final AnchorBlock anchorBlock) {
    this.anchorBlock = anchorBlock;
    this.blockLocation = anchorBlock.blockPos.asLocation();
    this.bottomLocation = this.blockLocation.clone().add(0.0, 1, 0.0);
    this.topLocation = this.blockLocation.clone().add(0.0, 2, 0.0);
    final Network network = Network.of(anchorBlock.networkId);
    this.bottomColour = network.bottom;
    this.topColour = network.top;

    this.activeBottomRingY = new double[ACTIVE_RING_COUNT];
    this.activeTopRingY = new double[ACTIVE_RING_COUNT];
    this.activeBottomRingColours = new Color[ACTIVE_RING_COUNT];
    this.activeTopRingColours = new Color[ACTIVE_RING_COUNT];
    this.preCalculateRingData(
        ACTIVE_RING_COUNT,
        this.activeBottomRingY,
        this.activeTopRingY,
        this.activeBottomRingColours,
        this.activeTopRingColours);

    this.inactiveBottomRingY = new double[INACTIVE_RING_COUNT];
    this.inactiveTopRingY = new double[INACTIVE_RING_COUNT];
    this.inactiveBottomRingColours = new Color[INACTIVE_RING_COUNT];
    this.inactiveTopRingColours = new Color[INACTIVE_RING_COUNT];
    this.preCalculateRingData(
        INACTIVE_RING_COUNT,
        this.inactiveBottomRingY,
        this.inactiveTopRingY,
        this.inactiveBottomRingColours,
        this.inactiveTopRingColours);

    this.ringRotationOffsets = new double[Math.max(ACTIVE_RING_COUNT, INACTIVE_RING_COUNT)];
    for (int ring = 0; ring < this.ringRotationOffsets.length; ring++) {
      this.ringRotationOffsets[ring] = ring * 0.2;
    }
  }

  // pre-calculates ring positions and colours for performance
  private void preCalculateRingData(
      final int ringCount,
      final double[] bottomRingY,
      final double[] topRingY,
      final Color[] bottomRingColours,
      final Color[] topRingColours) {
    for (int ring = 0; ring < ringCount; ring++) {
      final double ringFraction = (double) ring / ringCount;
      bottomRingY[ring] = this.bottomLocation.getY() + ringFraction;
      topRingY[ring] = this.topLocation.getY() + ringFraction;
      final double totalHeightFraction = ringFraction * 0.5;
      final double topHeightFraction = 0.5 + ringFraction * 0.5;
      bottomRingColours[ring] = this.getColourAtFraction(totalHeightFraction);
      topRingColours[ring] = this.getColourAtFraction(topHeightFraction);
    }
  }

  // disables visual effects and removes light sources
  void disable() {
    if (this.particleTask != null) {
      this.particleTask.cancel();
      this.particleTask = null;
    }
    this.updateLights(0);
  }

  // updates visual effects based on connection state
  void update() {
    final boolean isActive = this.anchorBlock.isRandom || this.anchorBlock.connectedTo != null;

    this.disable();

    // connected gateways emit light
    if (isActive) {
      this.updateLights(12);
    }

    // re-calculate nearby players
    this.nearbyPlayers.clear();
    for (final Player player : Bukkit.getOnlinePlayers()) {
      if (this.canSeeVisualsFrom(player.getLocation())) {
        this.addNearbyPlayer(player);
      }
    }

    this.particleTask =
        Schedule.periodically(
            2,
            () -> {
              if (!this.nearbyPlayers.isEmpty()) {
                this.spawnJavaParticles(
                    this.nearbyPlayers.stream()
                        .filter((final NearbyPlayer nearby) -> !nearby.isBedrock)
                        .map((final NearbyPlayer nearby) -> nearby.player)
                        .toList(),
                    isActive);
                this.spawnBedrockParticles(
                    this.nearbyPlayers.stream()
                        .filter((NearbyPlayer nearby) -> nearby.isBedrock)
                        .map((final NearbyPlayer nearby) -> nearby.player)
                        .toList());
              }
            });
  }

  // checks if player can see visuals from location
  private boolean canSeeVisualsFrom(final Location playerLoc) {
    return this.bottomLocation.getWorld() == playerLoc.getWorld()
        && this.bottomLocation.distanceSquared(playerLoc) <= VISIBLE_RANGE_SQUARED
        && !(playerLoc.getBlockX() == this.bottomLocation.getBlockX()
            && playerLoc.getBlockY() == this.bottomLocation.getBlockY()
            && playerLoc.getBlockZ() == this.bottomLocation.getBlockZ());
  }

  // updates nearby player tracking for visual effects
  void updateNearbyPlayer(final Player player, final Location location) {
    if (this.canSeeVisualsFrom(location)) {
      this.addNearbyPlayer(player);
    } else {
      this.removeNearbyPlayer(player);
    }
  }

  // adds player to nearby players list
  private void addNearbyPlayer(final Player player) {
    final NearbyPlayer nearbyPlayer = new NearbyPlayer(player);
    if (!this.nearbyPlayers.contains(nearbyPlayer)) {
      this.nearbyPlayers.add(nearbyPlayer);
    }
  }

  // removes player from nearby players list
  void removeNearbyPlayer(final Player player) {
    final Iterator<NearbyPlayer> iter = this.nearbyPlayers.iterator();
    while (iter.hasNext()) {
      if (iter.next().player.equals(player)) {
        iter.remove();
        break;
      }
    }
  }

  // spawns particle effects for java edition players
  private void spawnJavaParticles(final List<Player> players, final boolean isActive) {
    final World world = this.blockLocation.getWorld();

    final double baseRotation =
        world.getGameTime() * (isActive ? EFFECT_SPEED_ACTIVE : EFFECT_SPEED_INACTIVE);

    final Color[] bottomRingColours =
        isActive ? this.activeBottomRingColours : this.inactiveBottomRingColours;
    final Color[] topRingColours =
        isActive ? this.activeTopRingColours : this.inactiveTopRingColours;
    final double[] bottomRingY = isActive ? this.activeBottomRingY : this.inactiveBottomRingY;
    final double[] topRingY = isActive ? this.activeTopRingY : this.inactiveTopRingY;
    final int ringsPerSection = bottomRingColours.length;

    for (int ring = 0; ring < ringsPerSection; ring++) {
      for (int i = 0; i < EFFECT_PARTICLES; i++) {
        final double angle = baseRotation + this.ringRotationOffsets[ring] + (i * ANGLE_STEP);
        final double cosAngle = Math.cos(angle);
        final double sinAngle = Math.sin(angle);

        // bottom particle
        final double bottomX = this.bottomLocation.getX() + EFFECT_RADIUS * cosAngle;
        final double bottomZ = this.bottomLocation.getZ() + EFFECT_RADIUS * sinAngle;
        world.spawnParticle(
            Particle.TRAIL,
            players,
            null,
            bottomX,
            bottomRingY[ring],
            bottomZ,
            1,
            0.0,
            0.0,
            0.0,
            1,
            new Particle.Trail(
                new Location(world, bottomX, bottomRingY[ring], bottomZ),
                bottomRingColours[ring],
                isActive ? EFFECT_PARTICLES_TRAIL_ACTIVE : EFFECT_PARTICLES_TRAIL_INACTIVE),
            false);

        // top particle
        final double topX = this.topLocation.getX() + EFFECT_RADIUS * cosAngle;
        final double topZ = this.topLocation.getZ() + EFFECT_RADIUS * sinAngle;
        world.spawnParticle(
            Particle.TRAIL,
            players,
            null,
            topX,
            topRingY[ring],
            topZ,
            1,
            0.0,
            0.0,
            0.0,
            1,
            new Particle.Trail(
                new Location(world, topX, topRingY[ring], topZ),
                topRingColours[ring],
                isActive ? EFFECT_PARTICLES_TRAIL_ACTIVE : EFFECT_PARTICLES_TRAIL_INACTIVE),
            false);
      }
    }
  }

  // calculates colour gradient at specific height fraction
  private Color getColourAtFraction(final double fraction) {
    if (fraction <= 0.45) {
      // pure bottom colour (45%)
      return this.bottomColour.color;
    }
    if (fraction >= 0.55) {
      // pure top colour (45%)
      return this.topColour.color;
    }
    // gradient zone (10%)
    final double gradientFraction = (fraction - 0.45) / 0.1;
    final int topR = this.topColour.color.getRed();
    final int topG = this.topColour.color.getGreen();
    final int topB = this.topColour.color.getBlue();
    final int bottomR = this.bottomColour.color.getRed();
    final int bottomG = this.bottomColour.color.getGreen();
    final int bottomB = this.bottomColour.color.getBlue();
    return Color.fromRGB(
        (int) (bottomR + (topR - bottomR) * gradientFraction),
        (int) (bottomG + (topG - bottomG) * gradientFraction),
        (int) (bottomB + (topB - bottomB) * gradientFraction));
  }

  // spawns simplified particle effects for bedrock players
  private void spawnBedrockParticles(final List<Player> players) {
    final World world = this.blockLocation.getWorld();
    world.spawnParticle(
        Particle.DUST,
        players,
        null,
        this.topLocation.getX(),
        this.topLocation.getY(),
        this.topLocation.getZ(),
        1,
        0.0,
        0.0,
        0.0,
        1,
        new Particle.DustOptions(this.topColour.color, 1),
        false);
    world.spawnParticle(
        Particle.DUST,
        players,
        null,
        this.bottomLocation.getX(),
        this.bottomLocation.getY(),
        this.bottomLocation.getZ(),
        1,
        0.0,
        0.0,
        0.0,
        1,
        new Particle.DustOptions(this.bottomColour.color, 1),
        false);
  }

  // updates light blocks above anchor for visual effects
  private void updateLights(final int lightLevel) {
    final World world = this.blockLocation.getWorld();
    for (final Location loc : List.of(this.topLocation, this.bottomLocation)) {
      final Block block = world.getBlockAt(loc);
      if (lightLevel == 0) {
        block.setType(Material.AIR);
      } else {
        block.setType(Material.LIGHT);
        final Light light = (Light) block.getBlockData();
        light.setLevel(lightLevel);
        block.setBlockData(light);
      }
    }
  }
}
