package au.com.glob.clodmc.util;

import au.com.glob.clodmc.ClodMC;
import java.util.logging.Level;
import org.jetbrains.annotations.NotNull;

public final class Logger {
  public static void info(@NotNull String message) {
    ClodMC.instance.getLogger().info(message);
  }

  public static void warning(@NotNull String message) {
    ClodMC.instance.getLogger().warning(message);
  }

  public static void error(@NotNull String message) {
    ClodMC.instance.getLogger().severe(message);
  }

  public static void exception(@NotNull Throwable exception) {
    ClodMC.instance
        .getLogger()
        .log(
            Level.SEVERE,
            exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage(),
            exception);
  }
}
