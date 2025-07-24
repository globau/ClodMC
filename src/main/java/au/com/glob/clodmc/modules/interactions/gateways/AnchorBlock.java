package au.com.glob.clodmc.modules.interactions.gateways;

import au.com.glob.clodmc.util.Schedule;
import com.destroystokyo.paper.ParticleBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Light;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.NumberConversions;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@SerializableAs("ClodMC.AnchorBlock")
@NullMarked
public class AnchorBlock implements ConfigurationSerializable {
  private static final double EFFECT_RADIUS = 0.375;
  private static final double EFFECT_SPEED = 0.1;
  private static final int EFFECT_PARTICLES = 4;

  final int networkId;
  final BlockPos blockPos;
  final @Nullable String name;
  private final String displayName;

  private final Location topLocation;
  private final Location bottomLocation;
  final Colour topColour;
  final Colour bottomColour;

  @Nullable AnchorBlock connectedTo = null;
  private @Nullable BukkitTask particleTask = null;
  private final List<NearbyPlayer> nearbyPlayers = new ArrayList<>();
  final boolean isRandom;

  public AnchorBlock(int networkId, Location location, @Nullable String name) {
    this.networkId = networkId;
    this.blockPos = BlockPos.of(location);
    this.name = name;

    this.topLocation = location.clone().add(0.5, 2.5, 0.5);
    this.bottomLocation = location.clone().add(0.5, 1.5, 0.5);

    Network network = Gateways.networkIdToColours(networkId);
    this.topColour = network.top;
    this.bottomColour = network.bottom;

    this.displayName = this.getColourPair() + (this.name == null ? "" : " (" + this.name + ")");
    this.isRandom = networkId == Gateways.RANDOM_NETWORK_ID;
  }

  @Override
  public String toString() {
    return "AnchorBlock{"
        + "blockPos="
        + this.blockPos
        + ", topColour="
        + this.topColour
        + ", bottomColour="
        + this.bottomColour
        + ", connected="
        + (this.connectedTo != null)
        + '}';
  }

  String getInformation() {
    String prefix = "<yellow>" + this.displayName + "</yellow> - ";
    if (this.connectedTo != null) {
      return prefix
          + this.connectedTo.blockPos.getString(
              !this.blockPos.world.equals(this.connectedTo.blockPos.world));
    }
    if (this.networkId == Gateways.RANDOM_NETWORK_ID) {
      return prefix + "Random Location";
    }
    return prefix + "Disconnected";
  }

  String getColourPair() {
    return this.topColour.getDisplayName() + " :: " + this.bottomColour.getDisplayName();
  }

  void connectTo(AnchorBlock otherBlock) {
    this.connectedTo = otherBlock;
    otherBlock.connectedTo = this;
  }

  void disconnect() {
    if (this.connectedTo != null) {
      this.connectedTo.connectedTo = null;
    }
    this.connectedTo = null;
  }

  private Location facingLocation(Location location) {
    double yawRadians = Math.toRadians(location.getYaw());
    double facingX = location.getX() - Math.sin(yawRadians);
    double facingZ = location.getZ() + Math.cos(yawRadians);
    return new Location(location.getWorld(), facingX, location.getY(), facingZ);
  }

  private Block facingBlock(Location location) {
    return this.facingLocation(location).getBlock();
  }

  private boolean notFacingAnchor(Location location) {
    return !Gateways.instance.instances.containsKey(BlockPos.of(this.facingLocation(location)));
  }

  private boolean isFacingAir(Location location) {
    return this.facingBlock(location).isEmpty();
  }

  private boolean isFacingSolid(Location location) {
    return this.facingBlock(location).isSolid();
  }

  Location teleportLocation(Player player) {
    // rotate player to avoid facing a wall

    // get standing-on block, bottom, and top blocks for the player's location
    // snapped to 90 degrees of rotation
    Location blockLoc = this.blockPos.asLocation();
    blockLoc.setYaw((float) Math.round(player.getLocation().getYaw() / 90.0) * 90);
    blockLoc.setPitch(player.getLocation().getPitch());
    Location bottomLoc = blockLoc.clone().add(0, 1, 0);
    Location topLoc = bottomLoc.clone().add(0, 1, 0);

    // check for air; treat air blocks above other anchors as solid
    int attempts = 1;
    while (attempts <= 4
        && !(this.isFacingAir(bottomLoc)
            && this.isFacingAir(topLoc)
            && this.notFacingAnchor(blockLoc))) {
      blockLoc.setYaw(((blockLoc.getYaw() + 90) + 180) % 360 - 180);
      bottomLoc.setYaw(blockLoc.getYaw());
      topLoc.setYaw(blockLoc.getYaw());
      attempts++;
    }

    // didn't find air, try again without the anchor check
    if (!(this.isFacingAir(bottomLoc)
        && this.isFacingAir(topLoc)
        && this.notFacingAnchor(blockLoc))) {
      attempts = 1;
      while (attempts <= 4 && !(this.isFacingAir(bottomLoc) && this.isFacingAir(topLoc))) {
        bottomLoc.setYaw(((bottomLoc.getYaw() + 90) + 180) % 360 - 180);
        topLoc.setYaw(bottomLoc.getYaw());
        attempts++;
      }
    }

    // didn't find air, settle for non-solid
    if (!(this.isFacingAir(bottomLoc) && this.isFacingAir(topLoc))) {
      attempts = 1;
      while (attempts <= 4 && (this.isFacingSolid(bottomLoc) || this.isFacingSolid(topLoc))) {
        bottomLoc.setYaw(((bottomLoc.getYaw() + 90) + 180) % 360 - 180);
        topLoc.setYaw(bottomLoc.getYaw());
        attempts++;
      }
    }

    return bottomLoc;
  }

  void updateVisuals() {
    boolean isActive = this.isRandom || this.connectedTo != null;

    this.updateLights(isActive ? 12 : 0);

    // re-calculate nearby players
    this.nearbyPlayers.clear();
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (this.canSeeVisualsFrom(player.getLocation())) {
        this.addNearbyPlayer(player);
      }
    }

    if (this.particleTask != null) {
      this.particleTask.cancel();
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

  private void spawnJavaParticles(Collection<Player> players, boolean isActive) {
    double baseRotation = this.blockPos.world.getGameTime() * EFFECT_SPEED;
    double angleStep = 2 * Math.PI / EFFECT_PARTICLES;
    int ringsPerSection = isActive ? 8 : 4;

    World world = this.blockPos.world;
    Location bottomParticleLoc = new Location(world, 0, 0, 0);
    Location topParticleLoc = new Location(world, 0, 0, 0);

    for (int ring = 0; ring < ringsPerSection; ring++) {
      double ringFraction = (double) ring / ringsPerSection;
      double bottomY = this.bottomLocation.getY() + ringFraction - 0.5;
      double topY = this.topLocation.getY() + ringFraction - 0.5;
      double ringRotation = baseRotation + (ring * 0.2);

      for (int i = 0; i < EFFECT_PARTICLES; i++) {
        double angle = ringRotation + (i * angleStep);
        double cosAngle = Math.cos(angle);
        double sinAngle = Math.sin(angle);

        double x = this.bottomLocation.getX() + EFFECT_RADIUS * cosAngle;
        double z = this.bottomLocation.getZ() + EFFECT_RADIUS * sinAngle;
        bottomParticleLoc.setX(x);
        bottomParticleLoc.setY(bottomY);
        bottomParticleLoc.setZ(z);
        new ParticleBuilder(Particle.TRAIL)
            .data(new Particle.Trail(bottomParticleLoc, this.bottomColour.color, 5))
            .location(bottomParticleLoc)
            .receivers(players)
            .count(1)
            .spawn();

        x = this.topLocation.getX() + EFFECT_RADIUS * cosAngle;
        z = this.topLocation.getZ() + EFFECT_RADIUS * sinAngle;
        topParticleLoc.setX(x);
        topParticleLoc.setY(topY);
        topParticleLoc.setZ(z);
        new ParticleBuilder(Particle.TRAIL)
            .data(new Particle.Trail(topParticleLoc, this.topColour.color, 5))
            .location(topParticleLoc)
            .receivers(players)
            .count(1)
            .spawn();
      }
    }
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
    World world = this.blockPos.world;
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

  boolean canSeeVisualsFrom(Location playerLoc) {
    return this.bottomLocation.getWorld() == playerLoc.getWorld()
        && this.bottomLocation.distanceSquared(playerLoc) <= Gateways.VISIBLE_RANGE_SQUARED
        && !(playerLoc.getBlockX() == this.bottomLocation.getBlockX()
            && playerLoc.getBlockY() == this.bottomLocation.getBlockY()
            && playerLoc.getBlockZ() == this.bottomLocation.getBlockZ());
  }

  void addNearbyPlayer(Player player) {
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

  void stopVisuals() {
    if (this.particleTask != null) {
      this.particleTask.cancel();
      this.particleTask = null;
    }
    this.updateLights(0);
  }

  @Override
  public Map<String, Object> serialize() {
    // note: doesn't store adjustY
    Map<String, Object> serialised = new HashMap<>();
    serialised.put("title", this.displayName);
    serialised.put("world", this.blockPos.world.getName());
    serialised.put("x", this.blockPos.x);
    serialised.put("y", this.blockPos.y);
    serialised.put("z", this.blockPos.z);
    serialised.put("id", this.networkId);
    if (this.name != null) {
      serialised.put("name", this.name);
    }
    return serialised;
  }

  @SuppressWarnings("unused")
  public static AnchorBlock deserialize(Map<String, Object> args) {
    World world = Bukkit.getWorld((String) args.get("world"));
    if (world == null) {
      throw new IllegalArgumentException("unknown world");
    }

    return new AnchorBlock(
        NumberConversions.toInt(args.get("id")),
        new Location(
            world,
            NumberConversions.toInt(args.get("x")),
            NumberConversions.toInt(args.get("y")),
            NumberConversions.toInt(args.get("z"))),
        (String) args.get("name"));
  }
}
