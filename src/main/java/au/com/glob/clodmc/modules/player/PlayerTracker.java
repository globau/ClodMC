package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.datafile.PlayerDataFile;
import au.com.glob.clodmc.datafile.PlayerDataFiles;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Schedule;
import org.bukkit.Statistic;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jspecify.annotations.NullMarked;

/** Collect data about players */
@NullMarked
public class PlayerTracker implements Module, Listener {
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerJoin(PlayerJoinEvent event) {
    PlayerDataFile dataFile = PlayerDataFiles.of(event.getPlayer());
    dataFile.setPlayerName(event.getPlayer().getName());
    dataFile.touchLastLogin();
    dataFile.save();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerQuit(PlayerQuitEvent event) {
    PlayerDataFile dataFile = PlayerDataFiles.of(event.getPlayer());
    dataFile.touchLastLogout();
    dataFile.setPlaytimeMins(
        Math.round(event.getPlayer().getStatistic(Statistic.PLAY_ONE_MINUTE) / 20.0 / 60.0));
    dataFile.save();

    // unload player file after 10 seconds
    Schedule.delayed(
        10 * 20,
        () -> {
          if (!event.getPlayer().isOnline()) {
            PlayerDataFiles.unload(event.getPlayer());
          }
        });
  }
}
