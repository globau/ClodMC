package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.annotations.Audience;
import au.com.glob.clodmc.annotations.Doc;
import au.com.glob.clodmc.events.OpAlertEvent;
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

@Doc(
    audience = Audience.ADMIN,
    title = "Operator Alerts",
    description = "Collect startup alerts and send them to the first operator that logs in",
    hidden = true)
@NullMarked
public class OpAlerts implements Module, Listener {
  private final List<String> alerts = new ArrayList<>();

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onOpAlert(final OpAlertEvent event) {
    this.alerts.add(event.getMessage());
  }

  // send pending alerts to first op who joins
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerJoin(final PlayerJoinEvent event) {
    if (this.alerts.isEmpty() || !event.getPlayer().isOp()) {
      return;
    }

    Schedule.delayed(
        20,
        () -> {
          for (final String alert : this.alerts) {
            Chat.error(event.getPlayer(), "[ClodMC] %s".formatted(alert));
          }
          this.alerts.clear();
        });
  }
}
