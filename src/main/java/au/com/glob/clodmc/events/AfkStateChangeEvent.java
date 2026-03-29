package au.com.glob.clodmc.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NullMarked;

/** fired when a player's afk status changes */
@NullMarked
public class AfkStateChangeEvent extends Event {
  private static final HandlerList handlers = new HandlerList();

  private final Player player;
  private final boolean isAway;

  public AfkStateChangeEvent(final Player player, final boolean isAway) {
    this.player = player;
    this.isAway = isAway;
  }

  public Player getPlayer() {
    return this.player;
  }

  public boolean isAway() {
    return this.isAway;
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
