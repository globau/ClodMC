package au.com.glob.clodmc.modules.server;

import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.StringUtil;
import java.net.URI;
import org.bukkit.ServerLinks;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLinksSendEvent;
import org.jspecify.annotations.NullMarked;

/** Set server links in MC client pause screen */
@SuppressWarnings("UnstableApiUsage")
@NullMarked
public class ClodServerLinks implements Module, Listener {
  @EventHandler
  public void onPlayerLinksSend(PlayerLinksSendEvent event) {
    ServerLinks links = event.getLinks();
    links.addLink(
        StringUtil.asComponent("<yellow>Real-time Map</yellow>"),
        URI.create("https://clod.glob.au/"));
    links.addLink(
        StringUtil.asComponent("Server Status and Player Statistics"),
        URI.create("https://clod.glob.au/about/support"));
    links.addLink(
        StringUtil.asComponent("Email Glob"), URI.create("https://clod.glob.au/about/support"));
  }
}
