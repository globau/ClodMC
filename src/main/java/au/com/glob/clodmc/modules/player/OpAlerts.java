package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.Module;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.jetbrains.annotations.NotNull;

public class OpAlerts implements Module, Listener {
  @SuppressWarnings("NotNullFieldNotInitialized")
  private static @NotNull OpAlerts instance;

  private final @NotNull List<String> alerts = new ArrayList<>();

  public OpAlerts() {
    instance = this;
  }

  public static void addAlert(@NotNull String alert) {
    instance.alerts.add(alert);
  }

  @EventHandler
  public void onPlayerLogin(@NotNull PlayerLoginEvent event) {
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
