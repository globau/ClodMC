package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.Module;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public class OpAlerts implements Module, Listener {
  private final List<String> alerts = new ArrayList<>();

  private static OpAlerts instance;

  public OpAlerts() {
    instance = this;
  }

  public static void addAlert(String alert) {
    instance.alerts.add(alert);
  }

  @EventHandler
  public void onPlayerLogin(PlayerLoginEvent event) {
    if (this.alerts.isEmpty() || !event.getPlayer().isOp()) {
      return;
    }

    ClodMC.instance
        .getServer()
        .getScheduler()
        .scheduleSyncDelayedTask(
            ClodMC.instance,
            () -> {
              for (String alert : this.alerts) {
                event.getPlayer().sendRichMessage("[ClodMC] <red>" + alert + "</red>");
              }
            },
            20);
  }
}
