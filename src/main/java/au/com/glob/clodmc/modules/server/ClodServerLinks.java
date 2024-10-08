package au.com.glob.clodmc.modules.server;

import au.com.glob.clodmc.modules.Module;
import java.net.URI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.ServerLinks;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLinksSendEvent;
import org.jetbrains.annotations.NotNull;

/** Set server links in MC client pause screen */
@SuppressWarnings("UnstableApiUsage")
public class ClodServerLinks implements Module, Listener {
  @EventHandler
  public void onPlayerLinksSend(@NotNull PlayerLinksSendEvent event) {
    ServerLinks links = event.getLinks();
    links.addLink(
        MiniMessage.miniMessage().deserialize("<yellow>Real-time Map</yellow>"),
        URI.create("https://clod.glob.au/"));
    links.addLink(
        MiniMessage.miniMessage().deserialize("Server Status and Player Statistics"),
        URI.create("https://clod.glob.au/about/support"));
    links.addLink(
        MiniMessage.miniMessage().deserialize("Email Glob"),
        URI.create("https://clod.glob.au/about/support"));
  }
}
