package au.com.glob.clodmc.modules.server;

import au.com.glob.clodmc.annotations.Audience;
import au.com.glob.clodmc.annotations.Doc;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.StringUtil;
import java.net.URI;
import org.bukkit.ServerLinks;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLinksSendEvent;
import org.jspecify.annotations.NullMarked;

@Doc(
    audience = Audience.ADMIN,
    title = "Server Links",
    description = "Set server links in Minecraft client pause screen")
@SuppressWarnings("UnstableApiUsage")
@NullMarked
public class ClodServerLinks implements Module, Listener {
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerLinksSend(final PlayerLinksSendEvent event) {
    final ServerLinks links = event.getLinks();
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
