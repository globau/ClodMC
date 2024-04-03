package au.com.glob.clodmc.command;

import au.com.glob.clodmc.config.PlayerConfig;
import au.com.glob.clodmc.config.PluginConfig;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CommandUtil {
  private CommandUtil() {}

  public static @NotNull Player senderToPlayer(@NotNull CommandSender sender) throws CommandError {
    if (!(sender instanceof Player player)) {
      throw new CommandError("This command can only be run by a player");
    }
    return player;
  }

  public static @NotNull PlayerConfig getPlayerConfig(@NotNull Player player) throws CommandError {
    PlayerConfig playerConfig = PluginConfig.getInstance().getPlayerConfig(player);
    if (playerConfig == null) {
      throw new CommandError("Internal Error: no home data found");
    }
    return playerConfig;
  }
}
