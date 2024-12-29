package au.com.glob.clodmc.modules.bluemap;

import au.com.glob.clodmc.modules.Module;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class BlueMapUpdateEvent extends Event {
  private static final @NotNull HandlerList handlers = new HandlerList();
  private final @NotNull Class<? extends Module> sender;

  public BlueMapUpdateEvent(@NotNull Class<? extends Module> sender) {
    this.sender = sender;
  }

  public @NotNull Class<? extends Module> getSender() {
    return this.sender;
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return handlers;
  }

  @SuppressWarnings("unused")
  public static @NotNull HandlerList getHandlerList() {
    return handlers;
  }
}
