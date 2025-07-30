package au.com.glob.clodmc.modules.server;

import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.StringUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.jspecify.annotations.NullMarked;

/** Set MOTD automatically based on server type (prod, stage, dev) */
@NullMarked
public class MOTD implements Module, Listener {
  private final Component motd;

  public MOTD() {
    String hostname = System.getenv("HOSTNAME");
    if (hostname == null) {
      hostname = "";
    }
    String motd;
    if (hostname.equals("clod.glob.au")) {
      // prod
      motd = "<gold>clod-mc</gold> ∙ <blue>clod.glob.au";
    } else if (hostname.endsWith(".glob.au")) {
      // stage
      motd =
          "<gold>clod-mc</gold> ∙ "
              + hostname.substring(0, hostname.length() - ".glob.au".length());
    } else {
      // dev
      motd = "clod-mc ∙ dev";
    }
    this.motd = StringUtil.asComponent(motd);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onServerListPing(ServerListPingEvent event) {
    event.motd(this.motd);
  }
}
