package au.com.glob.clodmc;

import org.jetbrains.annotations.NotNull;

public class CommandError extends Exception {
  public CommandError(@NotNull String message) {
    super(message);
  }
}
