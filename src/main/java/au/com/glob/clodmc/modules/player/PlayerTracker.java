package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.config.PlayerConfig;
import au.com.glob.clodmc.config.PlayerConfigUpdater;
import au.com.glob.clodmc.modules.Module;
import java.time.LocalDateTime;
import org.bukkit.Statistic;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerTracker implements Module, Listener {
  @EventHandler
  public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
    try (PlayerConfigUpdater config = PlayerConfigUpdater.of(event.getPlayer())) {
      config.set("player.name", event.getPlayer().getName());
      config.set("player.last_login", LocalDateTime.now());
    }
  }

  @EventHandler
  public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
    try (PlayerConfigUpdater config = PlayerConfigUpdater.of(event.getPlayer())) {
      config.set("player.last_logout", LocalDateTime.now());
      config.set(
          "player.playtime_min",
          Math.round(event.getPlayer().getStatistic(Statistic.PLAY_ONE_MINUTE) / 20.0 / 60.0));
    }

    PlayerConfig.unload(event.getPlayer());
  }
}
