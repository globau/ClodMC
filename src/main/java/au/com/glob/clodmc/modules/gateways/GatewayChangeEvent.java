package au.com.glob.clodmc.modules.gateways;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GatewayChangeEvent extends Event {
  private static final @NotNull HandlerList handlers = new HandlerList();

  public @NotNull HandlerList getHandlers() {
    return handlers;
  }

  @SuppressWarnings("unused")
  public static @NotNull HandlerList getHandlerList() {
    return handlers;
  }
}
