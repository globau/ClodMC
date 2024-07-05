package au.com.glob.clodmc.modules.gateways;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.util.BlockPos;
import com.destroystokyo.paper.ParticleBuilder;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;

public class AnchorBlock implements ConfigurationSerializable {
  protected final BlockPos blockPos;
  protected final Location topLocation;
  protected final Location bottomLocation;
  protected final int networkId;
  protected final Color topColour;
  protected final Color bottomColour;

  protected AnchorBlock connectedTo = null;
  private BukkitTask particleTask = null;

  public AnchorBlock(int networkId, Location location) {
    this.networkId = networkId;
    this.blockPos = BlockPos.of(location);
    this.topLocation = location.clone().add(0.5, 2.5, 0.5);
    this.bottomLocation = location.clone().add(0.5, 1.5, 0.5);
    this.topColour = Config.idToTopColour(networkId);
    this.bottomColour = Config.idToBottomColour(networkId);
  }

  protected static boolean isAnchor(@NotNull ItemStack item) {
    ItemMeta meta = item.getItemMeta();
    return meta != null && meta.getPersistentDataContainer().has(Config.recipeKey);
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

  private Block facingBlock(@NotNull Location location) {
    double yawRadians = Math.toRadians(location.getYaw());
    double facingX = location.getX() - Math.sin(yawRadians);
    double facingZ = location.getZ() + Math.cos(yawRadians);
    Location facingLoc = new Location(location.getWorld(), facingX, location.getY(), facingZ);
    return facingLoc.getBlock();
  }

  private boolean isFacingAir(@NotNull Location location) {
    return this.facingBlock(location).isEmpty();
  }

  private boolean isFacingSolid(@NotNull Location location) {
    return this.facingBlock(location).isSolid();
  }

  protected Location teleportLocation(@NotNull Player player) {
    // rotate player to avoid facing a wall

    // get top and bottom blocks for the player's location
    // snapped to 90 degrees of rotation
    Location bottomLoc = this.blockPos.asLocation().add(0, 1, 0);
    bottomLoc.setYaw(Math.round(player.getLocation().getYaw() / 90.0) * 90);
    bottomLoc.setPitch(player.getLocation().getPitch());
    Location topLoc = bottomLoc.clone().add(0, 1, 0);

    // check for air
    int attempts = 1;
    while (attempts <= 4 && !(this.isFacingAir(bottomLoc) && this.isFacingAir(topLoc))) {
      bottomLoc.setYaw(((bottomLoc.getYaw() + 90) + 180) % 360 - 180);
      topLoc.setYaw(bottomLoc.getYaw());
      attempts++;
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

  protected void updateParticles() {
    this.stopParticles();

    if (this.connectedTo == null) {
      this.particleTask =
          Bukkit.getScheduler()
              .runTaskTimer(
                  ClodMC.instance,
                  () -> {
                    Collection<Player> players = this.getNearbyPlayers(8);
                    new ParticleBuilder(Particle.DUST)
                        .location(this.topLocation)
                        .receivers(players)
                        .data(new Particle.DustOptions(this.topColour, 1))
                        .count(1)
                        .spawn();
                    new ParticleBuilder(Particle.DUST)
                        .location(this.bottomLocation)
                        .receivers(players)
                        .data(new Particle.DustOptions(this.bottomColour, 1))
                        .count(1)
                        .spawn();
                  },
                  0,
                  20);
    } else {
      this.particleTask =
          Bukkit.getScheduler()
              .runTaskTimer(
                  ClodMC.instance,
                  () -> {
                    Collection<Player> players = this.getNearbyPlayers(12);
                    new ParticleBuilder(Particle.DUST)
                        .location(this.topLocation)
                        .data(new Particle.DustOptions(this.topColour, 2))
                        .receivers(players)
                        .count(5)
                        .spawn();
                    new ParticleBuilder(Particle.DUST)
                        .location(this.bottomLocation)
                        .data(new Particle.DustOptions(this.bottomColour, 2))
                        .receivers(players)
                        .count(5)
                        .spawn();
                  },
                  0,
                  20);
    }
  }

  private @NotNull Collection<Player> getNearbyPlayers(int radius) {
    // nearby players, excluding those standing on the anchor
    return this.bottomLocation
        .getWorld()
        .getNearbyPlayers(this.bottomLocation, radius, radius, radius)
        .stream()
        .filter(
            player -> {
              Location playerLoc = player.getLocation();
              return !(playerLoc.getWorld().equals(this.bottomLocation.getWorld())
                  && playerLoc.getBlockX() == this.bottomLocation.getBlockX()
                  && playerLoc.getBlockY() == this.bottomLocation.getBlockY()
                  && playerLoc.getBlockZ() == this.bottomLocation.getBlockZ());
            })
        .toList();
  }

  public void stopParticles() {
    if (this.particleTask != null) {
      this.particleTask.cancel();
      this.particleTask = null;
    }
  }

  @Override
  public @NotNull Map<String, Object> serialize() {
    // note: doesn't store adjustY
    Map<String, Object> serialised = new HashMap<>();
    serialised.put("world", this.blockPos.getWorld().getName());
    serialised.put("x", this.blockPos.getX());
    serialised.put("y", this.blockPos.getY());
    serialised.put("z", this.blockPos.getZ());
    serialised.put("id", this.networkId);
    return serialised;
  }

  @SuppressWarnings("unused")
  public static @NotNull AnchorBlock deserialize(@NotNull Map<String, Object> args) {
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
            NumberConversions.toInt(args.get("z"))));
  }
}
