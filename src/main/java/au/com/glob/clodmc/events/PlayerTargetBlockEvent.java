package au.com.glob.clodmc.events;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** fired when player changes view direction and targets a different block */
@NullMarked
public class PlayerTargetBlockEvent extends Event {
  private static final HandlerList handlers = new HandlerList();

  private final Player player;
  private final @Nullable Block targetBlock;

  public PlayerTargetBlockEvent(final Player player, final @Nullable Block targetBlock) {
    this.player = player;
    this.targetBlock = targetBlock;
  }

  public Player getPlayer() {
    return this.player;
  }

  public @Nullable Block getTargetBlock() {
    return this.targetBlock;
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
