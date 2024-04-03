package au.com.glob.clodmc.command;

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
}
