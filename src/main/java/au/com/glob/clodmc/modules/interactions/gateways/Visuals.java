package au.com.glob.clodmc.modules.interactions.gateways;

import au.com.glob.clodmc.util.Schedule;
import com.destroystokyo.paper.ParticleBuilder;
import java.util.ArrayList;
import java.util.Collection;
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

  Visuals(AnchorBlock anchorBlock) {
    this.anchorBlock = anchorBlock;
    this.blockLocation = anchorBlock.blockPos.asLocation();
    this.bottomLocation = this.blockLocation.clone().add(0.0, 1, 0.0);
    this.topLocation = this.blockLocation.clone().add(0.0, 2, 0.0);
    Network network = Gateways.networkIdToColours(anchorBlock.networkId);
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

  private void preCalculateRingData(
      int ringCount,
      double[] bottomRingY,
      double[] topRingY,
      Color[] bottomRingColours,
      Color[] topRingColours) {
    for (int ring = 0; ring < ringCount; ring++) {
      double ringFraction = (double) ring / ringCount;
      bottomRingY[ring] = this.bottomLocation.getY() + ringFraction;
      topRingY[ring] = this.topLocation.getY() + ringFraction;
      double totalHeightFraction = ringFraction * 0.5;
      double topHeightFraction = 0.5 + ringFraction * 0.5;
      bottomRingColours[ring] = this.getColourAtFraction(totalHeightFraction);
      topRingColours[ring] = this.getColourAtFraction(topHeightFraction);
    }
  }

  void disable() {
    if (this.particleTask != null) {
      this.particleTask.cancel();
      this.particleTask = null;
    }
    this.updateLights(0);
  }

  void update() {
    boolean isActive = this.anchorBlock.isRandom || this.anchorBlock.connectedTo != null;

    this.disable();

    // connected gateways emit light
    if (isActive) {
      this.updateLights(12);
    }

    // re-calculate nearby players
    this.nearbyPlayers.clear();
    for (Player player : Bukkit.getOnlinePlayers()) {
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
                        .filter((NearbyPlayer nearby) -> !nearby.isBedrock)
                        .map((NearbyPlayer nearby) -> nearby.player)
                        .toList(),
                    isActive);
                this.spawnBedrockParticles(
                    this.nearbyPlayers.stream()
                        .filter((NearbyPlayer nearby) -> nearby.isBedrock)
                        .map((NearbyPlayer nearby) -> nearby.player)
                        .toList());
              }
            });
  }

  private boolean canSeeVisualsFrom(Location playerLoc) {
    return this.bottomLocation.getWorld() == playerLoc.getWorld()
        && this.bottomLocation.distanceSquared(playerLoc) <= VISIBLE_RANGE_SQUARED
        && !(playerLoc.getBlockX() == this.bottomLocation.getBlockX()
            && playerLoc.getBlockY() == this.bottomLocation.getBlockY()
            && playerLoc.getBlockZ() == this.bottomLocation.getBlockZ());
  }

  void updateNearbyPlayer(Player player, Location location) {
    if (this.canSeeVisualsFrom(location)) {
      this.addNearbyPlayer(player);
    } else {
      this.removeNearbyPlayer(player);
    }
  }

  private void addNearbyPlayer(Player player) {
    NearbyPlayer nearbyPlayer = new NearbyPlayer(player);
    if (!this.nearbyPlayers.contains(nearbyPlayer)) {
      this.nearbyPlayers.add(nearbyPlayer);
    }
  }

  void removeNearbyPlayer(Player player) {
    Iterator<NearbyPlayer> iter = this.nearbyPlayers.iterator();
    while (iter.hasNext()) {
      if (iter.next().player.equals(player)) {
        iter.remove();
        break;
      }
    }
  }

  private void spawnJavaParticles(Collection<Player> players, boolean isActive) {
    World world = this.blockLocation.getWorld();
    Location particleLoc = new Location(world, 0, 0, 0);

    double baseRotation =
        world.getGameTime() * (isActive ? EFFECT_SPEED_ACTIVE : EFFECT_SPEED_INACTIVE);

    Color[] bottomRingColours =
        isActive ? this.activeBottomRingColours : this.inactiveBottomRingColours;
    Color[] topRingColours = isActive ? this.activeTopRingColours : this.inactiveTopRingColours;
    double[] bottomRingY = isActive ? this.activeBottomRingY : this.inactiveBottomRingY;
    double[] topRingY = isActive ? this.activeTopRingY : this.inactiveTopRingY;
    int ringsPerSection = bottomRingColours.length;

    for (int ring = 0; ring < ringsPerSection; ring++) {
      double ringRotation = baseRotation + this.ringRotationOffsets[ring];
      Color bottomSectionColour = bottomRingColours[ring];
      Color topSectionColour = topRingColours[ring];

      for (int i = 0; i < EFFECT_PARTICLES; i++) {
        double angle = ringRotation + (i * ANGLE_STEP);
        double cosAngle = Math.cos(angle);
        double sinAngle = Math.sin(angle);

        particleLoc.setX(this.bottomLocation.getX() + EFFECT_RADIUS * cosAngle);
        particleLoc.setY(bottomRingY[ring]);
        particleLoc.setZ(this.bottomLocation.getZ() + EFFECT_RADIUS * sinAngle);
        new ParticleBuilder(Particle.TRAIL)
            .data(
                new Particle.Trail(
                    particleLoc,
                    bottomSectionColour,
                    isActive ? EFFECT_PARTICLES_TRAIL_ACTIVE : EFFECT_PARTICLES_TRAIL_INACTIVE))
            .location(particleLoc)
            .receivers(players)
            .count(1)
            .spawn();

        particleLoc.setX(this.topLocation.getX() + EFFECT_RADIUS * cosAngle);
        particleLoc.setY(topRingY[ring]);
        particleLoc.setZ(this.topLocation.getZ() + EFFECT_RADIUS * sinAngle);
        new ParticleBuilder(Particle.TRAIL)
            .data(
                new Particle.Trail(
                    particleLoc,
                    topSectionColour,
                    isActive ? EFFECT_PARTICLES_TRAIL_ACTIVE : EFFECT_PARTICLES_TRAIL_INACTIVE))
            .location(particleLoc)
            .receivers(players)
            .count(1)
            .spawn();
      }
    }
  }

  private Color getColourAtFraction(double fraction) {
    if (fraction <= 0.45) {
      // pure bottom colour (45%)
      return this.bottomColour.color;
    }
    if (fraction >= 0.55) {
      // pure top colour (45%)
      return this.topColour.color;
    }
    // gradient zone (10%)
    double gradientFraction = (fraction - 0.45) / 0.1;
    int topR = this.topColour.color.getRed();
    int topG = this.topColour.color.getGreen();
    int topB = this.topColour.color.getBlue();
    int bottomR = this.bottomColour.color.getRed();
    int bottomG = this.bottomColour.color.getGreen();
    int bottomB = this.bottomColour.color.getBlue();
    return Color.fromRGB(
        (int) (bottomR + (topR - bottomR) * gradientFraction),
        (int) (bottomG + (topG - bottomG) * gradientFraction),
        (int) (bottomB + (topB - bottomB) * gradientFraction));
  }

  private void spawnBedrockParticles(List<Player> players) {
    new ParticleBuilder(Particle.DUST)
        .data(new Particle.DustOptions(this.topColour.color, 1))
        .location(this.topLocation)
        .receivers(players)
        .count(1)
        .spawn();
    new ParticleBuilder(Particle.DUST)
        .data(new Particle.DustOptions(this.bottomColour.color, 1))
        .location(this.bottomLocation)
        .receivers(players)
        .count(1)
        .spawn();
  }

  private void updateLights(int lightLevel) {
    World world = this.blockLocation.getWorld();
    for (Location loc : List.of(this.topLocation, this.bottomLocation)) {
      Block block = world.getBlockAt(loc);
      if (lightLevel == 0) {
        block.setType(Material.AIR);
      } else {
        block.setType(Material.LIGHT);
        Light light = (Light) block.getBlockData();
        light.setLevel(lightLevel);
        block.setBlockData(light);
      }
    }
  }
}
