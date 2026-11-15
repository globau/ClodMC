package au.com.glob.clodmc.modules.interactions.gateways;

import au.com.glob.clodmc.util.Players;
import au.com.glob.clodmc.util.Schedule;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
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

/** manages visual effects for gateway anchors */
@NullMarked
public class Visuals {
  private static final int VISIBLE_RANGE_SQUARED = 32 * 32;

  private static final int BEDROCK_PARTICLE_UPDATE_TICKS = 2;
  private static final int BEDROCK_PARTICLES_PER_RING = 4;

  // a random gateway's eligibility split only changes as players' playtime crosses the
  // threshold, so it doesn't need re-checking on every update
  private static final int ELIGIBILITY_REFRESH_TICKS = 20;

  private final AnchorBlock anchorBlock;
  private final Location blockLocation;
  private final Location topLocation;
  private final Location bottomLocation;
  final Colour topColour;
  final Colour bottomColour;

  private boolean isActive = false;
  private @Nullable BukkitTask rotationTask = null;
  private @Nullable BukkitTask bedrockParticleTask = null;
  private @Nullable BukkitTask eligibilityTask = null;
  private final List<NearbyPlayer> nearbyPlayers = new ArrayList<>();
  private final Set<Player> cachedIneligiblePlayers = new HashSet<>();
  private @Nullable Helix activeHelix = null;
  private @Nullable Helix inactiveHelix = null;
  private boolean awaitingChunk = false;

  Visuals(final AnchorBlock anchorBlock) {
    this.anchorBlock = anchorBlock;
    this.blockLocation = anchorBlock.blockPos.asLocation();
    this.bottomLocation = this.blockLocation.clone().add(0.0, 1, 0.0);
    this.topLocation = this.blockLocation.clone().add(0.0, 2, 0.0);
    final Network network = Network.of(anchorBlock.networkId);
    this.bottomColour = network.bottom;
    this.topColour = network.top;
  }

  // disables visual effects and removes light sources
  void disable() {
    if (this.rotationTask != null) {
      this.rotationTask.cancel();
      this.rotationTask = null;
    }
    if (this.bedrockParticleTask != null) {
      this.bedrockParticleTask.cancel();
      this.bedrockParticleTask = null;
    }
    this.stopEligibilityTask();
    this.removeHelices();
    this.awaitingChunk = false;
    this.updateLights(0);
  }

  // updates visual effects based on connection state
  void update() {
    this.isActive = this.anchorBlock.isRandom || this.anchorBlock.connectedTo != null;

    this.disable();

    // connected gateways emit light
    if (this.isActive) {
      this.updateLights(12);
    }

    // re-calculate nearby players
    this.nearbyPlayers.clear();
    for (final Player player : Bukkit.getOnlinePlayers()) {
      if (this.canSeeVisualsFrom(player.getLocation(), Players.isBedrock(player))) {
        this.addNearbyPlayer(player);
      }
    }

    this.rotationTask = Schedule.periodically(Helix.ROTATION_UPDATE_TICKS, this::rotateHelices);

    this.bedrockParticleTask =
        Schedule.periodically(
            BEDROCK_PARTICLE_UPDATE_TICKS,
            () -> {
              final List<Player> bedrockPlayers =
                  this.nearbyPlayers.stream()
                      .filter((final NearbyPlayer nearby) -> nearby.isBedrock)
                      .map((final NearbyPlayer nearby) -> nearby.player)
                      .toList();
              if (!bedrockPlayers.isEmpty()) {
                this.spawnBedrockParticles(bedrockPlayers);
              }
            });
  }

  // re-checks nearby players not yet known to be ineligible for a random gateway; once a
  // player is found ineligible they can't become eligible again, so they're never re-checked
  private void updateIneligiblePlayers() {
    boolean changed = false;
    for (final NearbyPlayer nearby : this.nearbyPlayers) {
      if (nearby.isBedrock || this.cachedIneligiblePlayers.contains(nearby.player)) {
        continue;
      }
      if (!RandomTeleport.isEligible(nearby.player)) {
        this.cachedIneligiblePlayers.add(nearby.player);
        changed = true;
      }
    }

    if (changed) {
      this.refreshHelices();
    }
    if (!this.hasPlayerNeedingRecheck()) {
      this.stopEligibilityTask();
    }
  }

  // true if a nearby java player's eligibility isn't yet known to be permanently ineligible
  private boolean hasPlayerNeedingRecheck() {
    for (final NearbyPlayer nearby : this.nearbyPlayers) {
      if (!nearby.isBedrock && !this.cachedIneligiblePlayers.contains(nearby.player)) {
        return true;
      }
    }
    return false;
  }

  private void startEligibilityTask() {
    if (this.eligibilityTask == null) {
      this.eligibilityTask =
          Schedule.periodically(ELIGIBILITY_REFRESH_TICKS, this::updateIneligiblePlayers);
    }
  }

  private void stopEligibilityTask() {
    if (this.eligibilityTask != null) {
      this.eligibilityTask.cancel();
      this.eligibilityTask = null;
    }
  }

  // checks if player can see visuals from location; the glass shards are see-through, but
  // bedrock's particles would obscure the view of a player standing on the anchor
  private boolean canSeeVisualsFrom(final Location playerLoc, final boolean isBedrock) {
    return this.bottomLocation.getWorld().equals(playerLoc.getWorld())
        && this.bottomLocation.distanceSquared(playerLoc) <= VISIBLE_RANGE_SQUARED
        && !(isBedrock
            && playerLoc.getBlockX() == this.bottomLocation.getBlockX()
            && playerLoc.getBlockY() == this.bottomLocation.getBlockY()
            && playerLoc.getBlockZ() == this.bottomLocation.getBlockZ());
  }

  // updates nearby player tracking for visual effects
  void updateNearbyPlayer(final Player player, final Location location) {
    if (this.canSeeVisualsFrom(location, Players.isBedrock(player))) {
      this.addNearbyPlayer(player);
    } else {
      this.removeNearbyPlayer(player);
    }
  }

  // adds player to nearby players list
  private void addNearbyPlayer(final Player player) {
    final NearbyPlayer nearbyPlayer = new NearbyPlayer(player);
    if (this.nearbyPlayers.contains(nearbyPlayer)) {
      return;
    }
    this.nearbyPlayers.add(nearbyPlayer);
    if (nearbyPlayer.isBedrock) {
      return;
    }
    if (this.anchorBlock.isRandom) {
      if (RandomTeleport.isEligible(player)) {
        this.startEligibilityTask();
      } else {
        this.cachedIneligiblePlayers.add(player);
      }
    }
    this.refreshHelices();
  }

  // removes player from nearby players list
  void removeNearbyPlayer(final Player player) {
    final Iterator<NearbyPlayer> iter = this.nearbyPlayers.iterator();
    while (iter.hasNext()) {
      final NearbyPlayer nearby = iter.next();
      if (nearby.player.equals(player)) {
        iter.remove();
        if (!nearby.isBedrock) {
          this.refreshHelices();
        }
        break;
      }
    }
    if (this.anchorBlock.isRandom && !this.hasPlayerNeedingRecheck()) {
      this.stopEligibilityTask();
    }
  }

  // the random gateway is only active for eligible viewers; every other gateway's active state
  // is the same for everyone
  private boolean showsActiveTo(final Player player) {
    return this.anchorBlock.isRandom
        ? !this.cachedIneligiblePlayers.contains(player)
        : this.isActive;
  }

  // spawns, removes, and re-assigns viewers of the active and inactive helices so each nearby
  // java player sees exactly one of them
  private void refreshHelices() {
    final List<Player> activeViewers = new ArrayList<>();
    final List<Player> inactiveViewers = new ArrayList<>();
    for (final NearbyPlayer nearby : this.nearbyPlayers) {
      if (!nearby.isBedrock) {
        (this.showsActiveTo(nearby.player) ? activeViewers : inactiveViewers).add(nearby.player);
      }
    }
    this.activeHelix = this.syncHelix(this.activeHelix, activeViewers, HelixStyle.ACTIVE);
    this.inactiveHelix = this.syncHelix(this.inactiveHelix, inactiveViewers, HelixStyle.INACTIVE);
    this.awaitingChunk =
        !(activeViewers.isEmpty() && inactiveViewers.isEmpty())
            && !this.bottomLocation.isChunkLoaded();
  }

  // returns the helix for the given viewers, spawning or removing it as needed; shards spawned
  // into an unloaded chunk are discarded immediately, so none are spawned until it loads
  private @Nullable Helix syncHelix(
      @Nullable final Helix helix, final List<Player> viewers, final HelixStyle style) {
    final boolean wanted = !viewers.isEmpty() && this.bottomLocation.isChunkLoaded();
    Helix result = helix;
    if (result != null && (!wanted || !result.isValid())) {
      result.remove();
      result = null;
    }
    if (!wanted) {
      return null;
    }
    if (result == null) {
      result = new Helix(this.bottomLocation, this.bottomColour, this.topColour, style);
    }
    result.setViewers(viewers);
    return result;
  }

  // removes both helices and their shards
  private void removeHelices() {
    if (this.activeHelix != null) {
      this.activeHelix.remove();
      this.activeHelix = null;
    }
    if (this.inactiveHelix != null) {
      this.inactiveHelix.remove();
      this.inactiveHelix = null;
    }
  }

  // advances both helices one rotation step, respawning any that lost shards or were skipped
  // while the chunk was unloaded
  private void rotateHelices() {
    if (this.awaitingChunk && this.bottomLocation.isChunkLoaded()) {
      this.refreshHelices();
    }

    if (this.activeHelix == null && this.inactiveHelix == null) {
      return;
    }

    // shards are non-persistent, so they vanish if their chunk unloads
    if ((this.activeHelix != null && !this.activeHelix.isValid())
        || (this.inactiveHelix != null && !this.inactiveHelix.isValid())) {
      this.refreshHelices();
    }

    final long gameTime = this.blockLocation.getWorld().getGameTime();
    if (this.activeHelix != null) {
      this.activeHelix.rotate(gameTime);
    }
    if (this.inactiveHelix != null) {
      this.inactiveHelix.rotate(gameTime);
    }
  }

  // bedrock players can't see the shards, so they get a ring of dust per section that turns in
  // step with them instead
  private void spawnBedrockParticles(final List<Player> players) {
    final HelixStyle style = this.isActive ? HelixStyle.ACTIVE : HelixStyle.INACTIVE;
    final double spin = this.blockLocation.getWorld().getGameTime() * style.speed();
    spawnBedrockRing(players, this.bottomLocation, this.bottomColour, spin);
    spawnBedrockRing(players, this.topLocation, this.topColour, spin);
  }

  private static void spawnBedrockRing(
      final List<Player> players, final Location centre, final Colour colour, final double spin) {
    final World world = centre.getWorld();
    final Particle.DustOptions dust = new Particle.DustOptions(colour.color, 1);
    for (int i = 0; i < BEDROCK_PARTICLES_PER_RING; i++) {
      final double angle = spin + i * 2 * Math.PI / BEDROCK_PARTICLES_PER_RING;
      world.spawnParticle(
          Particle.DUST,
          players,
          null,
          centre.getX() + Helix.RADIUS * Math.cos(angle),
          centre.getY() + 0.5,
          centre.getZ() + Helix.RADIUS * Math.sin(angle),
          1,
          0.0,
          0.0,
          0.0,
          1,
          dust,
          false);
    }
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
