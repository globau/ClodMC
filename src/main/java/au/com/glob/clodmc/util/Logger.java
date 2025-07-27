package au.com.glob.clodmc.util;

import au.com.glob.clodmc.ClodMC;
import java.util.logging.Level;
import org.jspecify.annotations.NullMarked;

/** logfile helpers */
@NullMarked
public final class Logger {
  public static void info(String message) {
    ClodMC.instance.getLogger().info(message);
  }

  public static void warning(String message) {
    ClodMC.instance.getLogger().warning(message);
  }

  public static void error(String message) {
    ClodMC.instance.getLogger().severe(message);
  }

  public static void exception(Throwable exception) {
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
