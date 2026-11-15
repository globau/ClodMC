package au.com.glob.clodmc.modules.interactions.gateways;

import au.com.glob.clodmc.ClodMC;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.NullMarked;

/** a rotating helix of glass shards above a gateway anchor, visible only to its viewers */
@NullMarked
final class Helix {
  static final float RADIUS = 0.38f;

  // the client interpolates translation in a straight line, so each step must stay short enough
  // for the chord across the arc to be indistinguishable from the arc itself
  static final int ROTATION_UPDATE_TICKS = 5;

  private static final int SHARDS_PER_STRAND = 8;
  private static final double SHARD_PHASE_STEP = 2 * Math.PI / SHARDS_PER_STRAND;
  private static final float SHARD_RISE = 0.2f;
  private static final float SHARD_THICKNESS = 0.08f;
  private static final float SHARD_HEIGHT = 0.6f;
  private static final float SHARD_WIDTH = 0.35f;
  private static final Vector3f SHARD_SCALE =
      new Vector3f(SHARD_THICKNESS, SHARD_HEIGHT, SHARD_WIDTH);
  private static final Quaternionf NO_ROTATION = new Quaternionf();

  private final HelixStyle style;
  private final List<Shard> shards = new ArrayList<>();
  private final Set<Player> viewers = new HashSet<>();

  Helix(
      final Location base,
      final Colour bottomColour,
      final Colour topColour,
      final HelixStyle style) {
    this.style = style;
    final World world = base.getWorld();
    final double spin = this.spinAt(world.getGameTime());
    for (int strand = 0; strand < style.strands(); strand++) {
      final double strandOffset = strand * 2 * Math.PI / style.strands();
      for (int i = 0; i < SHARDS_PER_STRAND; i++) {
        final double phase = strandOffset + i * SHARD_PHASE_STEP;
        final float height = i * SHARD_RISE;
        final Material glass = i < SHARDS_PER_STRAND / 2 ? bottomColour.glass : topColour.glass;
        final BlockDisplay display =
            world.spawn(
                base,
                BlockDisplay.class,
                (final BlockDisplay d) -> {
                  d.setPersistent(false);
                  d.setVisibleByDefault(false);
                  d.setBlock(glass.createBlockData());
                  d.setBrightness(style.brightness());
                  d.setTransformation(shardTransformation(phase + spin, height));
                });
        this.shards.add(new Shard(display, phase, height));
      }
    }
  }

  // wrapped to keep float precision once converted for the client
  private double spinAt(final long gameTime) {
    return (gameTime * this.style.speed()) % (2 * Math.PI);
  }

  // starts interpolating every shard towards where it should be when the next update fires
  void rotate(final long gameTime) {
    final double spin = this.spinAt(gameTime + ROTATION_UPDATE_TICKS);
    for (final Shard shard : this.shards) {
      shard.display().setInterpolationDelay(0);
      shard.display().setInterpolationDuration(ROTATION_UPDATE_TICKS);
      shard.display().setTransformation(shardTransformation(shard.phase() + spin, shard.height()));
    }
  }

  // shows the shards to the given players and hides them from everyone else
  void setViewers(final List<Player> players) {
    for (final Player player : List.copyOf(this.viewers)) {
      if (!players.contains(player)) {
        this.viewers.remove(player);
        for (final Shard shard : this.shards) {
          player.hideEntity(ClodMC.instance, shard.display());
        }
      }
    }
    for (final Player player : players) {
      if (this.viewers.add(player)) {
        for (final Shard shard : this.shards) {
          player.showEntity(ClodMC.instance, shard.display());
        }
      }
    }
  }

  // true if every shard still exists in the world
  boolean isValid() {
    for (final Shard shard : this.shards) {
      if (!shard.display().isValid()) {
        return false;
      }
    }
    return true;
  }

  // removes all shards from the world
  void remove() {
    for (final Shard shard : this.shards) {
      shard.display().remove();
    }
    this.shards.clear();
    this.viewers.clear();
  }

  // a block display renders from its origin corner, so the shard is offset outwards by the
  // radius and centred on its width, then the offset and the shard are rotated together around
  // the column's axis
  private static Transformation shardTransformation(final double angle, final float height) {
    final Quaternionf rotation = new Quaternionf().rotationY((float) -angle);
    final Vector3f translation = new Vector3f(RADIUS, height, -SHARD_WIDTH / 2).rotate(rotation);
    return new Transformation(translation, rotation, SHARD_SCALE, NO_ROTATION);
  }
}
