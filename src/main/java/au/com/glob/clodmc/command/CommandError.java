package au.com.glob.clodmc.command;

import org.bukkit.command.CommandException;

public class CommandError extends CommandException {
  public CommandError(String message) {
    super(message);
  }

  public CommandError(Exception e) {
    super(e.getMessage());
  }
}
