package au.com.glob.clodmc.util;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** generic wrapper for http response data */
@NullMarked
public class HttpResponse<T> {
  private final @Nullable T response;

  public HttpResponse(@Nullable final T response) {
    this.response = response;
  }

  // get the wrapped response data
  public @Nullable T getResponse() {
    return this.response;
  }
}
