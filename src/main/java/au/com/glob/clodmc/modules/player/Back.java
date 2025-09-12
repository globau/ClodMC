package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.annotations.Audience;
import au.com.glob.clodmc.annotations.Doc;
import au.com.glob.clodmc.command.CommandBuilder;
import au.com.glob.clodmc.command.CommandError;
import au.com.glob.clodmc.datafile.PlayerDataFile;
import au.com.glob.clodmc.datafile.PlayerDataFiles;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.TeleportUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jspecify.annotations.NullMarked;

@Doc(
    audience = Audience.PLAYER,
    title = "Back",
    description = "/back command to return to where you last teleported from")
@NullMarked
public class Back implements Module, Listener {
  public Back() {
    CommandBuilder.build("back")
        .description("Teleport to previous location")
        .executor(
            (final Player player) -> {
              final PlayerDataFile dataFile = PlayerDataFiles.of(player);
              final Location location = (Location) dataFile.get("back");
              if (location == null) {
                throw new CommandError("No previous location");
              }
              TeleportUtil.teleport(player, location, "back");
            });
  }

  // store previous location when player teleports via command
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerTeleport(final PlayerTeleportEvent event) {
    if (event.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND) {
      final PlayerDataFile dataFile = PlayerDataFiles.of(event.getPlayer());
      dataFile.set("back", event.getPlayer().getLocation());
      dataFile.save();
    }
  }
}
