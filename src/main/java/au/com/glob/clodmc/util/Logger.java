package au.com.glob.clodmc.util;

import au.com.glob.clodmc.ClodMC;
import java.util.logging.Level;
import org.jspecify.annotations.NullMarked;

/** logfile helpers */
@NullMarked
public final class Logger {
  // log info message
  public static void info(final String message) {
    ClodMC.instance.getLogger().info(message);
  }

  // log warning message
  public static void warning(final String message) {
    ClodMC.instance.getLogger().warning(message);
  }

  // log error message
  public static void error(final String message) {
    ClodMC.instance.getLogger().severe(message);
  }

  public static void error(final String message, final Exception e) {
    ClodMC.instance.getLogger().severe("%s: %s".formatted(message, e.getMessage()));
  }

  // log exception with stack trace
  public static void exception(final Throwable exception) {
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
