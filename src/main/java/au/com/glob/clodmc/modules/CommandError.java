package au.com.glob.clodmc.modules;

import org.bukkit.command.CommandException;
import org.jetbrains.annotations.NotNull;

public class CommandError extends CommandException {
  public CommandError(@NotNull String message) {
    super(message);
  }
}
