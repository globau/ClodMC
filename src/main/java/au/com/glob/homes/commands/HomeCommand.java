package au.com.glob.homes.commands;

import au.com.glob.homes.BaseCommand;
import au.com.glob.homes.CommandError;
import au.com.glob.homes.config.PlayerConfig;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HomeCommand extends BaseCommand {
  @Override
  protected void execute(
      @NotNull Player player, @NotNull PlayerConfig playerConfig, @NotNull String[] args)
      throws CommandError {
    String homeName = args.length == 0 ? PlayerConfig.DEFAULT_NAME : args[0];

    Location location = playerConfig.getHome(homeName);
    if (location == null) {
      throw new CommandError(
          homeName.equals(PlayerConfig.DEFAULT_NAME)
              ? "No home set"
              : "No such home '" + homeName + "'");
    }

    player.sendRichMessage(
        "<grey>Teleporting you "
            + (homeName.equals(PlayerConfig.DEFAULT_NAME) ? "home" : "to '" + homeName + "'")
            + "</grey>");
    playerConfig.setHome(PlayerConfig.BACK_NAME, player.getLocation());
    player.teleportAsync(location);
  }
}
