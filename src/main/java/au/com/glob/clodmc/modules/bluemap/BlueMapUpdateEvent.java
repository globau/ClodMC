package au.com.glob.clodmc.modules.bluemap;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class BlueMapUpdateEvent extends Event {
  private static final @NotNull HandlerList handlers = new HandlerList();
  private final @NotNull BlueMapSource source;

  public BlueMapUpdateEvent(@NotNull BlueMapSource source) {
    this.source = source;
  }

  public @NotNull BlueMapSource getSource() {
    return this.source;
  }

  public @NotNull HandlerList getHandlers() {
    return handlers;
  }

  @SuppressWarnings("unused")
  public static @NotNull HandlerList getHandlerList() {
    return handlers;
  }
}
