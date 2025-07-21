package au.com.glob.clodmc.util;

import au.com.glob.clodmc.ClodMC;
import java.util.logging.Level;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** logfile helpers */
@NullMarked
public final class Logger {
  public static void info(@Nullable String message) {
    ClodMC.instance.getLogger().info(message);
  }

  public static void warning(@Nullable String message) {
    if (message != null) {
      ClodMC.instance.getLogger().warning(message);
    }
  }

  public static void error(@Nullable String message) {
    if (message != null) {
      ClodMC.instance.getLogger().severe(message);
    }
  }

  public static void exception(@Nullable Throwable exception) {
    if (exception != null) {
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
}
