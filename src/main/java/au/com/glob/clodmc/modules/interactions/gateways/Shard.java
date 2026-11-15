package au.com.glob.clodmc.modules.interactions.gateways;

import org.bukkit.entity.BlockDisplay;
import org.jspecify.annotations.NullMarked;

/** a single glass shard in a gateway helix, with its fixed position along the helix */
@NullMarked
record Shard(BlockDisplay display, double phase, float height) {}
