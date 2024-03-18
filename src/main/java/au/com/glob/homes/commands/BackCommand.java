package au.com.glob.homes.commands;

import au.com.glob.homes.BaseCommand;
import au.com.glob.homes.CommandError;
import au.com.glob.homes.config.PlayerConfig;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BackCommand extends BaseCommand {
  @Override
  protected void execute(
      @NotNull Player player, @NotNull PlayerConfig playerConfig, @NotNull String[] args)
      throws CommandError {
    Location location = playerConfig.getHome(PlayerConfig.BACK_NAME);
    if (location == null) {
      throw new CommandError("No previous location");
    }

    player.sendRichMessage("<grey>Teleporting you back</grey>");
    playerConfig.setHome(PlayerConfig.BACK_NAME, player.getLocation());
    player.teleportAsync(location);
  }
}
