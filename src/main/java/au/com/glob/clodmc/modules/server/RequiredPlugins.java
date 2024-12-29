package au.com.glob.clodmc.modules.server;

import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.player.OpAlerts;
import au.com.glob.clodmc.util.Logger;
import au.com.glob.clodmc.util.StringUtil;
import java.util.List;
import java.util.StringJoiner;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.jetbrains.annotations.NotNull;

/** Don't allow non-op players to connect unless all required plugins are loaded */
public class RequiredPlugins implements Listener, Module {
  private static final @NotNull List<String> REQUIRED = List.of("GriefPrevention");

  private boolean preventLogin = true;

  @EventHandler
  public void onServerLoad(@NotNull ServerLoadEvent event) {
    StringJoiner missing = new StringJoiner(" ");
    for (String name : REQUIRED) {
      if (!Bukkit.getPluginManager().isPluginEnabled(name)) {
        missing.add(name);
      }
    }

    this.preventLogin = missing.length() > 0;
    if (this.preventLogin) {
      String alert = "Missing required plugin(s): " + missing;
      OpAlerts.addAlert(alert);
      Logger.error("\n***\n*** " + alert + "\n***");
    }
  }

  @EventHandler
  public void onPlayerLogin(@NotNull PlayerLoginEvent event) {
    if (!this.preventLogin || event.getPlayer().isOp()) {
      return;
    }

    event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
    event.kickMessage(StringUtil.asComponent("A required plugin is not loaded"));
  }
}
