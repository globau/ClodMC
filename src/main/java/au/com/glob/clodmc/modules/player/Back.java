package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.command.CommandBuilder;
import au.com.glob.clodmc.command.CommandError;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Chat;
import au.com.glob.clodmc.util.PlayerDataFile;
import au.com.glob.clodmc.util.PlayerDataUpdater;
import au.com.glob.clodmc.util.PlayerLocation;
import au.com.glob.clodmc.util.TeleportUtil;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;

/** /back command to return to where you last teleported from */
public class Back implements Module, Listener {
  public Back() {
    CommandBuilder.build("back")
        .description("Teleport to previous location")
        .executor(
            (@NotNull Player player) -> {
              PlayerDataFile config = PlayerDataFile.of(player);
              PlayerLocation location = (PlayerLocation) config.get("back");
              if (location == null) {
                throw new CommandError("No previous location");
              }

              Chat.fyi(player, "Teleporting you back");
              location.teleportPlayer(player);
            })
        .register();
  }

  @Override
  public void loadConfig() {
    for (UUID uuid : PlayerDataFile.knownUUIDs()) {
      try (PlayerDataUpdater config = PlayerDataUpdater.of(uuid)) {
        PlayerLocation location = (PlayerLocation) config.get("homes_internal.back");
        if (location != null) {
          config.set("back", location);
          config.remove("homes_internal");
        }
      }
    }
  }

  @EventHandler
  public void onPlayerTeleport(@NotNull PlayerTeleportEvent event) {
    if (event.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND) {
      try (PlayerDataUpdater config = PlayerDataUpdater.of(event.getPlayer())) {
        config.set("back", PlayerLocation.of(TeleportUtil.getStandingPos(event.getPlayer())));
      }
    }
  }
}
