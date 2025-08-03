package au.com.glob.clodmc.modules.interactions.gateways;

import au.com.glob.clodmc.util.BlockPos;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.bukkit.util.NumberConversions;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@SerializableAs("ClodMC.AnchorBlock")
@NullMarked
public class AnchorBlock implements ConfigurationSerializable {
  final int networkId;
  final BlockPos blockPos;
  final @Nullable String name;
  private final String displayName;

  final Colour topColour;
  final Colour bottomColour;

  @Nullable AnchorBlock connectedTo = null;
  final Visuals visuals;
  final boolean isRandom;

  public AnchorBlock(int networkId, Location location, @Nullable String name) {
    this.networkId = networkId;
    this.blockPos = BlockPos.of(location);
    this.name = name;

    Network network = Gateways.networkIdToColours(networkId);
    this.topColour = network.top;
    this.bottomColour = network.bottom;

    this.displayName =
        "%s%s"
            .formatted(this.getColourPair(), this.name == null ? "" : " (%s)".formatted(this.name));
    this.isRandom = networkId == Gateways.RANDOM_NETWORK_ID;
    this.visuals = new Visuals(this);
  }

  @Override
  public String toString() {
    return "AnchorBlock{blockPos=%s, topColour=%s, bottomColour=%s, connected=%s}"
        .formatted(this.blockPos, this.topColour, this.bottomColour, this.connectedTo != null);
  }

  String getInformation() {
    String prefix = "<yellow>%s</yellow> - ".formatted(this.displayName);
    if (this.connectedTo != null) {
      return "%s%s"
          .formatted(
              prefix,
              this.connectedTo.blockPos.getString(
                  !this.blockPos.world.equals(this.connectedTo.blockPos.world)));
    }
    if (this.networkId == Gateways.RANDOM_NETWORK_ID) {
      return "%sRandom Location".formatted(prefix);
    }
    return "%sDisconnected".formatted(prefix);
  }

  String getColourPair() {
    return "%s :: %s"
        .formatted(this.topColour.getDisplayName(), this.bottomColour.getDisplayName());
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
    Location blockLoc = this.blockPos.asLocation();
    blockLoc.setPitch(player.getLocation().getPitch());
    Location bottomLoc = blockLoc.clone().add(0, 1, 0);
    Location topLoc = bottomLoc.clone().add(0, 1, 0);

    // snap yaws to 90 degrees of rotation
    float playerYaw = Math.round(player.getLocation().getYaw() / 90.0) * 90;
    float[] yaws = new float[4];
    float y = playerYaw;
    for (int i = 0; i < 4; i++) {
      yaws[i] = y;
      y = ((y + 90) + 180) % 360 - 180;
    }

    // air blocks, avoid anchors
    for (float yaw : yaws) {
      blockLoc.setYaw(yaw);
      bottomLoc.setYaw(yaw);
      topLoc.setYaw(yaw);
      if (this.isFacingAir(bottomLoc)
          && this.isFacingAir(topLoc)
          && this.notFacingAnchor(blockLoc)) {
        return bottomLoc;
      }
    }

    // non-solid, avoid anchors
    for (float yaw : yaws) {
      blockLoc.setYaw(yaw);
      bottomLoc.setYaw(yaw);
      topLoc.setYaw(yaw);
      if (!this.isFacingSolid(bottomLoc)
          && !this.isFacingSolid(topLoc)
          && this.notFacingAnchor(blockLoc)) {
        return bottomLoc;
      }
    }

    // air, anchors ok
    for (float yaw : yaws) {
      blockLoc.setYaw(yaw);
      bottomLoc.setYaw(yaw);
      topLoc.setYaw(yaw);
      if (!this.isFacingAir(bottomLoc) && !this.isFacingAir(topLoc)) {
        return bottomLoc;
      }
    }

    // non-solid, anchors ok
    for (float yaw : yaws) {
      blockLoc.setYaw(yaw);
      bottomLoc.setYaw(yaw);
      topLoc.setYaw(yaw);
      if (!this.isFacingSolid(bottomLoc) && !this.isFacingSolid(topLoc)) {
        return bottomLoc;
      }
    }

    // original rotation
    blockLoc.setYaw(playerYaw);
    return bottomLoc;
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
