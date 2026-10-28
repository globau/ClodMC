package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.annotations.Audience;
import au.com.glob.clodmc.annotations.Doc;
import au.com.glob.clodmc.datafile.PlayerDataFile;
import au.com.glob.clodmc.datafile.PlayerDataFiles;
import au.com.glob.clodmc.modules.Module;
import org.bukkit.Statistic;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jspecify.annotations.NullMarked;

@Doc(
    audience = Audience.SERVER,
    title = "Player Tracker",
    description = "Collect data about players (eg. login/logout times)")
@NullMarked
public class PlayerTracker extends Module implements Listener {
  // update player data on join
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerJoin(final PlayerJoinEvent event) {
    PlayerDataFiles.update(
        event.getPlayer(),
        (final PlayerDataFile dataFile) -> {
          dataFile.setPlayerName(event.getPlayer().getName());
          dataFile.touchLastLogin();
        });
  }

  // update player data on quit
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerQuit(final PlayerQuitEvent event) {
    PlayerDataFiles.update(
        event.getPlayer(),
        (final PlayerDataFile dataFile) -> {
          dataFile.touchLastLogout();
          dataFile.setPlaytimeMins(
              Math.round(event.getPlayer().getStatistic(Statistic.PLAY_ONE_MINUTE) / 20.0 / 60.0));
        });
  }
}
