package au.com.glob.clodmc.events;

import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NullMarked;

/** fired when a block is removed outside of a normal block break event */
@NullMarked
public class BlockRemovedEvent extends Event {
  private static final HandlerList handlers = new HandlerList();

  private final Block block;

  public BlockRemovedEvent(final Block block) {
    this.block = block;
  }

  public Block getBlock() {
    return this.block;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  @SuppressWarnings("unused")
  public static HandlerList getHandlerList() {
    return handlers;
  }
}
