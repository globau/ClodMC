package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.CommandError;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.SimpleCommand;
import au.com.glob.clodmc.util.PlayerDataFile;
import au.com.glob.clodmc.util.PlayerDataUpdater;
import au.com.glob.clodmc.util.PlayerLocation;
import java.util.List;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;

public class BackCommand extends SimpleCommand implements Module, Listener {
  public BackCommand() {
    super("back", "Teleport to previous location");
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
    try (PlayerDataUpdater config = PlayerDataUpdater.of(event.getPlayer())) {
      config.set("back", PlayerLocation.of(event.getFrom()));
    }
  }

  @Override
  protected void execute(@NotNull CommandSender sender, @NotNull List<String> args) {
    Player player = this.toPlayer(sender);

    PlayerDataFile config = PlayerDataFile.of(player);
    PlayerLocation location = (PlayerLocation) config.get("back");
    if (location == null) {
      throw new CommandError("No previous location");
    }

    ClodMC.fyi(player, "Teleporting you back");
    location.teleportPlayer(player);
  }
}
