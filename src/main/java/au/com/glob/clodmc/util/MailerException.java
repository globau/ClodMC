package au.com.glob.clodmc.util;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** exception for email sending failures */
@NullMarked
public class MailerException extends Exception {
  public MailerException(@Nullable String message) {
    super(message);
  }
}
