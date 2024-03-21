package au.com.glob.clodmc.commands;

import au.com.glob.clodmc.BaseCommand;
import au.com.glob.clodmc.CommandError;
import au.com.glob.clodmc.config.PlayerConfig;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DelHomeCommand extends BaseCommand {
  @Override
  protected void execute(
      @NotNull Player player, @NotNull PlayerConfig playerConfig, @NotNull String[] args)
      throws CommandError {
    String name = args.length == 0 ? PlayerConfig.DEFAULT_NAME : args[0];
    Location location = playerConfig.getHomeLocation(name);
    if (location == null) {
      throw new CommandError("No such home '" + name + "'");
    }
    playerConfig.deleteHome(name);
    player.sendRichMessage("<grey>Home '" + name + "' deleted</grey>");
  }

  @Override
  protected List<String> tabComplete(
      @NotNull Player player, @NotNull PlayerConfig playerConfig, @NotNull String[] args) {
    return this.tabCompleteHomes(playerConfig, args);
  }
}
