package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Chat;
import au.com.glob.clodmc.util.Schedule;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jspecify.annotations.NullMarked;

/** Collect startup alerts and send them to the first operator that logs in */
@NullMarked
public class OpAlerts implements Module, Listener {
  @SuppressWarnings({"NotNullFieldNotInitialized", "NullAway.Init"})
  private static OpAlerts instance;

  private final List<String> alerts = new ArrayList<>();

  public OpAlerts() {
    instance = this;
  }

  // add an alert to be shown to first op who joins
  public static void addAlert(String alert) {
    instance.alerts.add(alert);
  }

  // send pending alerts to first op who joins
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerJoin(PlayerJoinEvent event) {
    if (this.alerts.isEmpty() || !event.getPlayer().isOp()) {
      return;
    }

    Schedule.delayed(
        20,
        () -> {
          for (String alert : this.alerts) {
            Chat.error(event.getPlayer(), "[ClodMC] %s".formatted(alert));
          }
          this.alerts.clear();
        });
  }
}
