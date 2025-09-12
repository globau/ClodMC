package au.com.glob.clodmc.command;

import org.bukkit.command.CommandException;
import org.jspecify.annotations.NullMarked;

/** exception thrown when a command encounters an error during execution */
@NullMarked
public class CommandError extends CommandException {
  public CommandError(final String message) {
    super(message);
  }
}
