package au.com.glob.clodmc.modules;

import org.bukkit.command.CommandException;

public class CommandError extends CommandException {
  public CommandError(String message) {
    super(message);
  }

  public CommandError(Exception e) {
    super(e.getMessage());
  }
}
