package au.com.glob.clodmc.util;

import java.util.List;
import org.jspecify.annotations.NullMarked;

/** exception for configuration validation errors */
@NullMarked
public class InvalidConfigException extends Exception {
  private final List<String> errors;

  public InvalidConfigException(final List<String> errors) {
    this.errors = errors;
  }

  @Override
  public String getMessage() {
    return String.join("\n", this.errors);
  }

  // log all validation errors to the server log
  public void logErrors() {
    for (final String line : this.errors) {
      Logger.error(line);
    }
  }
}
