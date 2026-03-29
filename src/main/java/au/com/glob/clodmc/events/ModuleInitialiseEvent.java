package au.com.glob.clodmc.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NullMarked;

/** fired after all modules are registered, signalling the config loading phase */
@NullMarked
public class ModuleInitialiseEvent extends Event {
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
