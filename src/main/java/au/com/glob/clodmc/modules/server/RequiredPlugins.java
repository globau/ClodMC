package au.com.glob.clodmc.modules.server;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.player.OpAlerts;
import java.util.List;
import java.util.StringJoiner;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.server.ServerLoadEvent;

public class RequiredPlugins implements Listener, Module {
  private boolean preventLogin = true;

  @EventHandler
  public void onServerLoaded(ServerLoadEvent event) {
    List<String> required =
        ClodMC.instance.getConfig().getStringList("required-plugins").stream().sorted().toList();

    StringJoiner missing = new StringJoiner(" ");
    for (String name : required) {
      if (!Bukkit.getPluginManager().isPluginEnabled(name)) {
        missing.add(name);
      }
    }

    this.preventLogin = missing.length() > 0;
    if (this.preventLogin) {
      String alert = "Missing required plugin(s): " + missing;
      OpAlerts.addAlert(alert);
      ClodMC.logError("\n***\n*** " + alert + "\n***");
    }
  }

  @EventHandler
  public void onPlayerLogin(PlayerLoginEvent event) {
    if (!this.preventLogin || event.getPlayer().isOp()) {
      return;
    }

    event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
    event.kickMessage(MiniMessage.miniMessage().deserialize("A required plugin is not loaded"));
  }
}
