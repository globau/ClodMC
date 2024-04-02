package au.com.glob.clodmc.modules.homes;

import au.com.glob.clodmc.command.CommandError;
import au.com.glob.clodmc.command.PlayerCommand;
import au.com.glob.clodmc.config.PlayerConfig;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HomeCommand extends PlayerCommand {
  @Override
  protected void execute(
      @NotNull Player player, @NotNull PlayerConfig playerConfig, @NotNull String[] args)
      throws CommandError {
    String homeName = args.length == 0 ? PlayerConfig.DEFAULT_NAME : args[0];

    Location location = playerConfig.getHomeLocation(homeName);
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
    playerConfig.setBackLocation(player.getLocation());
    player.teleportAsync(location);
  }

  @Override
  protected List<String> tabComplete(
      @NotNull Player player, @NotNull PlayerConfig playerConfig, @NotNull String[] args) {
    return this.completeFrom(playerConfig.getHomeNames(), args);
  }
}
