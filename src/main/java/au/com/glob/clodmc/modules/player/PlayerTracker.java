package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.PlayerDataFile;
import au.com.glob.clodmc.util.PlayerDataUpdater;
import au.com.glob.clodmc.util.Schedule;
import org.bukkit.Statistic;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

/** Collect data about players */
public class PlayerTracker implements Module, Listener {
  @EventHandler
  public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
    try (PlayerDataUpdater config = PlayerDataUpdater.of(event.getPlayer())) {
      config.setPlayerName(event.getPlayer().getName());
      config.touchLastLogin();
    }
  }

  @EventHandler
  public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
    try (PlayerDataUpdater config = PlayerDataUpdater.of(event.getPlayer())) {
      config.touchLastLogout();
      config.setPlaytimeMins(
          Math.round(event.getPlayer().getStatistic(Statistic.PLAY_ONE_MINUTE) / 20.0 / 60.0));
    }

    // unload player file after 10 seconds
    Schedule.delayed(
        10 * 20,
        () -> {
          if (!event.getPlayer().isOnline()) {
            PlayerDataFile.unload(event.getPlayer());
          }
        });
  }
}
