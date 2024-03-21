package au.com.glob.clodmc.commands;

import au.com.glob.clodmc.BaseCommand;
import au.com.glob.clodmc.CommandError;
import au.com.glob.clodmc.config.PlayerConfig;
import au.com.glob.clodmc.config.PluginConfig;
import java.util.List;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SetHomeCommand extends BaseCommand {
  @Override
  protected void execute(
      @NotNull Player player, @NotNull PlayerConfig playerConfig, @NotNull String[] args)
      throws CommandError {

    String name = args.length == 0 ? PlayerConfig.DEFAULT_NAME : args[0];
    if (name.equals(PlayerConfig.BACK_NAME)) {
      throw new CommandError("Illegal home name '" + name + "'");
    }
    int maxHomes = PluginConfig.getInstance().getMaxHomes();
    if (playerConfig.getHomes().size() >= maxHomes) {
      throw new CommandError("You have reached the maximum number of homes (" + maxHomes + ")");
    }

    boolean existing = playerConfig.getHome(name) != null;
    playerConfig.setHome(name, player.getLocation());
    if (name.equals(PlayerConfig.DEFAULT_NAME)) {
      player.sendMessage("Home set to current location");
    } else {
      player.sendMessage("Home '" + name + "' " + (existing ? "updated" : "created"));
    }
  }

  @Override
  protected List<String> tabComplete(
      @NotNull Player player, @NotNull PlayerConfig playerConfig, @NotNull String[] args) {
    return List.of();
  }
}
