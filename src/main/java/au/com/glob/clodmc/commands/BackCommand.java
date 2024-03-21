package au.com.glob.clodmc.commands;

import au.com.glob.clodmc.BaseCommand;
import au.com.glob.clodmc.CommandError;
import au.com.glob.clodmc.config.PlayerConfig;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BackCommand extends BaseCommand {
  @Override
  protected void execute(
      @NotNull Player player, @NotNull PlayerConfig playerConfig, @NotNull String[] args)
      throws CommandError {
    Location location = playerConfig.getBackLocation();
    if (location == null) {
      throw new CommandError("No previous location");
    }

    player.sendRichMessage("<grey>Teleporting you back</grey>");
    playerConfig.setBackLocation(player.getLocation());
    player.teleportAsync(location);
  }

  @Override
  protected List<String> tabComplete(
      @NotNull Player player, @NotNull PlayerConfig playerConfig, @NotNull String[] args) {
    return List.of();
  }
}
