package au.com.glob.clodmc.util;

import java.util.List;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class InvalidConfigException extends Exception {
  private final List<String> errors;

  public InvalidConfigException(List<String> errors) {
    this.errors = errors;
  }

  @Override
  public String getMessage() {
    return String.join("\n", this.errors);
  }

  public void logErrors() {
    for (String line : this.errors) {
      Logger.error(line);
    }
  }
}
