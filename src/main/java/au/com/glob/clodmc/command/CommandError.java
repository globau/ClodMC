package au.com.glob.clodmc.command;

import org.bukkit.command.CommandException;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class CommandError extends CommandException {
  public CommandError(String message) {
    super(message);
  }
}
