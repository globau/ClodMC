package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Chat;
import au.com.glob.clodmc.util.Schedule;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

/** Collect startup alerts and send them to the first operator that logs in */
public class OpAlerts implements Module, Listener {
  @SuppressWarnings({"NotNullFieldNotInitialized", "NullAway.Init"})
  private static @NotNull OpAlerts instance;

  private final @NotNull List<String> alerts = new ArrayList<>();

  public OpAlerts() {
    instance = this;
  }

  public static void addAlert(@NotNull String alert) {
    instance.alerts.add(alert);
  }

  @EventHandler
  public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
    if (this.alerts.isEmpty() || !event.getPlayer().isOp()) {
      return;
    }

    Schedule.delayed(
        20,
        () -> {
          for (String alert : this.alerts) {
            Chat.error(event.getPlayer(), "[ClodMC] " + alert);
          }
          this.alerts.clear();
        });
  }
}
