package au.com.glob.clodmc.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NullMarked;

/** fired when an alert should be shown to ops */
@NullMarked
public class OpAlertEvent extends Event {
  private static final HandlerList handlers = new HandlerList();

  private final String message;

  public OpAlertEvent(final String message) {
    this.message = message;
  }

  public String getMessage() {
    return this.message;
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
