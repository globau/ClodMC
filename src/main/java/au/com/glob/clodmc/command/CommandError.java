package au.com.glob.clodmc.command;

import org.jetbrains.annotations.NotNull;

public class CommandError extends Exception {
  public CommandError(@NotNull String message) {
    super(message);
  }
}
