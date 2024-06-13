package au.com.glob.clodmc.modules.server;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.Module;
import java.util.Set;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.Plugin;

public class RequiredPlugins implements Listener, Module {
  private final Set<String> required;
  private Set<String> missing;
  private final Component kickMessage;

  public RequiredPlugins() {
    super();

    this.required =
        ClodMC.instance.getConfig().getStringList("required-plugins").stream()
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
    this.kickMessage =
        MiniMessage.miniMessage().deserialize("<red>A required plugin is not loaded</red>");
    this.updateMissing();
  }

  private void updateMissing() {
    this.missing =
        this.required.stream()
            .filter(
                (String name) -> {
                  Plugin p = ClodMC.instance.getServer().getPluginManager().getPlugin(name);
                  return p == null || !p.isEnabled();
                })
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
  }

  @EventHandler
  public void onPluginEnable(PluginEnableEvent event) {
    this.missing.remove(event.getPlugin().getName().toLowerCase());
  }

  @EventHandler
  public void onPluginDisable(PluginDisableEvent event) {
    String pluginName = event.getPlugin().getName();
    if (!this.required.contains(pluginName.toLowerCase())) {
      return;
    }

    this.missing.add(pluginName.toLowerCase());

    for (Player player : ClodMC.instance.getServer().getOnlinePlayers()) {
      if (player.isOp()) {
        player.sendRichMessage("<red>Plugin " + pluginName + " is not loaded</red>");
      } else {
        player.kick(this.kickMessage);
      }
    }
  }

  @EventHandler
  public void onPlayerLogin(PlayerLoginEvent event) {
    if (this.missing.isEmpty()) {
      return;
    }

    if (event.getPlayer().isOp()) {
      ClodMC.instance
          .getServer()
          .getScheduler()
          .scheduleSyncDelayedTask(
              ClodMC.instance,
              () ->
                  event
                      .getPlayer()
                      .sendRichMessage("<red>" + this.missingErrorMessage() + "</red>"),
              20);
    } else {
      event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
      event.kickMessage(this.kickMessage);
    }
  }

  @EventHandler
  public void onServerLoaded(ServerLoadEvent event) {
    this.updateMissing();
    if (!this.missing.isEmpty()) {
      ClodMC.logError(this.missingErrorMessage());
    }
  }

  private String missingErrorMessage() {
    return "Missing required plugins: "
        + this.missing.stream()
            .sorted(String::compareToIgnoreCase)
            .collect(Collectors.joining(" "));
  }
}
