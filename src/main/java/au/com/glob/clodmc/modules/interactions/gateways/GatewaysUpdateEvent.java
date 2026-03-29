package au.com.glob.clodmc.modules.interactions.gateways;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NullMarked;

/** triggered when gateways are created/destroyed */
@NullMarked
public class GatewaysUpdateEvent extends Event {
  private static final HandlerList handlers = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  @SuppressWarnings("unused")
  public static HandlerList getHandlerList() {
    return handlers;
  }
}
