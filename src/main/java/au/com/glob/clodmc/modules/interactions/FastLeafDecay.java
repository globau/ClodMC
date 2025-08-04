package au.com.glob.clodmc.modules.interactions;

// from https://github.com/StarTux/FastLeafDecay

import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Schedule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.jspecify.annotations.NullMarked;

/** Nearly instant decaying of leafs */
@NullMarked
public class FastLeafDecay implements Listener, Module {
  private final List<Block> scheduledBlocks = new ArrayList<>();

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    // start trying to break leaves immediately after a log is broken
    this.onBlockRemove(event.getBlock(), 5);
  }

  // cascade leaf decay when leaves naturally decay
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onLeavesDecay(LeavesDecayEvent event) {
    // check neighbours when a leaf decays to trigger a cascade
    this.onBlockRemove(event.getBlock(), 2);
  }

  // schedule neighbouring leaves to decay after a delay
  private void onBlockRemove(final Block oldBlock, long delay) {
    // block broken must be either a log of leaf
    if (!Tag.LOGS.isTagged(oldBlock.getType()) && !Tag.LEAVES.isTagged(oldBlock.getType())) {
      return;
    }

    // schedule neighbouring leaves to decay
    List<BlockFace> neighbours =
        new ArrayList<>(
            List.of(
                BlockFace.UP,
                BlockFace.NORTH,
                BlockFace.EAST,
                BlockFace.SOUTH,
                BlockFace.WEST,
                BlockFace.DOWN));
    Collections.shuffle(neighbours);

    // schedule neighbouring leaf blocks for decay, in a random order
    for (BlockFace neighbourFace : neighbours) {
      final Block block = oldBlock.getRelative(neighbourFace);
      if (!Tag.LEAVES.isTagged(block.getType())) {
        continue;
      }
      Leaves leaves = (Leaves) block.getBlockData();
      if (leaves.isPersistent() || this.scheduledBlocks.contains(block)) {
        continue;
      }
      Schedule.delayed(delay, () -> this.decay(block));
      this.scheduledBlocks.add(block);
    }
  }

  // decay a single leaf block with particles and sound
  private void decay(Block block) {
    // make sure we're decaying a loaded leaf block
    if (!this.scheduledBlocks.remove(block)
        || !block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)
        || !Tag.LEAVES.isTagged(block.getType())) {
      return;
    }

    // and it's within 7 blocks from a log
    Leaves leaves = (Leaves) block.getBlockData();
    if (leaves.isPersistent() || leaves.getDistance() < 7) {
      return;
    }

    // allow other plugins to cancel decay
    @SuppressWarnings("UnstableApiUsage")
    LeavesDecayEvent event = new LeavesDecayEvent(block);
    Bukkit.getServer().getPluginManager().callEvent(event);
    if (event.isCancelled()) {
      return;
    }

    // break the block, with particles and sound
    block
        .getWorld()
        .spawnParticle(
            Particle.BLOCK,
            block.getLocation().add(0.5, 0.5, 0.5),
            8,
            0.2,
            0.2,
            0.2,
            0,
            block.getType().createBlockData());

    block
        .getWorld()
        .playSound(block.getLocation(), Sound.BLOCK_GRASS_BREAK, SoundCategory.BLOCKS, 0.05f, 1.2f);

    block.breakNaturally();
  }
}
