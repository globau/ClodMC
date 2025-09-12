package au.com.glob.clodmc.modules.server;

import au.com.glob.clodmc.annotations.Audience;
import au.com.glob.clodmc.annotations.Doc;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.player.OpAlerts;
import au.com.glob.clodmc.util.Logger;
import au.com.glob.clodmc.util.StringUtil;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.jspecify.annotations.NullMarked;

@Doc(
    audience = Audience.ADMIN,
    title = "Required Plugins",
    description = "Don't allow non-op players to connect unless all required plugins are loaded")
@NullMarked
public class RequiredPlugins implements Listener, Module {
  private static final List<String> REQUIRED = List.of("GriefPrevention");

  private boolean preventLogin = true;

  // check for required plugins on server load
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onServerLoad(final ServerLoadEvent event) {
    final StringJoiner missing = new StringJoiner(" ");
    for (final String name : REQUIRED) {
      if (!Bukkit.getPluginManager().isPluginEnabled(name)) {
        missing.add(name);
      }
    }

    this.preventLogin = missing.length() > 0;
    if (this.preventLogin) {
      final String alert = "Missing required plugin(s): %s".formatted(missing);
      OpAlerts.addAlert(alert);
      Logger.error("\n***\n*** %s\n***".formatted(alert));
    }
  }

  // prevent non-op login if required plugins missing
  @SuppressWarnings("UnstableApiUsage")
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onPlayerConnectionValidateLogin(final PlayerConnectionValidateLoginEvent event) {
    if (!this.preventLogin) {
      return;
    }

    if (event.getConnection() instanceof final PlayerConfigurationConnection connection) {
      final UUID uuid = connection.getProfile().getId();
      if (uuid != null) {
        final OfflinePlayer player = Bukkit.getServer().getOfflinePlayer(uuid);
        if (!player.isOp()) {
          event.kickMessage(StringUtil.asComponent("A required plugin is not loaded"));
        }
      }
    }
  }
}
