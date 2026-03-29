package au.com.glob.clodmc.modules.bluemap;

import de.bluecolored.bluemap.api.BlueMapAPI;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NullMarked;

/** init event for bluemap addons */
@NullMarked
public class BlueMapInitEvent extends Event {
  private static final HandlerList handlers = new HandlerList();

  private final BlueMapAPI api;

  public BlueMapInitEvent(final BlueMapAPI api) {
    this.api = api;
  }

  public BlueMapAPI getApi() {
    return this.api;
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
