package au.com.glob.clodmc.command;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CommandUtil {
  private CommandUtil() {}

  public static @NotNull Player senderToPlayer(@NotNull CommandSender sender)
      throws WrapperCommandSyntaxException {
    if (!(sender instanceof Player player)) {
      throw CommandAPI.failWithString("This command can only be run by a player");
    }
    return player;
  }
}
