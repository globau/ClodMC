package au.com.glob.homes.commands;

import au.com.glob.homes.BaseCommand;
import au.com.glob.homes.CommandError;
import au.com.glob.homes.config.PlayerConfig;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DelHomeCommand extends BaseCommand {
  @Override
  protected void execute(
      @NotNull Player player, @NotNull PlayerConfig playerConfig, @NotNull String[] args)
      throws CommandError {
    String name = args.length == 0 ? PlayerConfig.DEFAULT_NAME : args[0];
    Location location = playerConfig.getHome(name);
    if (location == null) {
      throw new CommandError("No such home '" + name + "'");
    }
    playerConfig.setHome(name, null);
    player.sendRichMessage("<grey>Home '" + name + "' deleted</grey>");
  }
}
