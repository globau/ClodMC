package au.com.glob.clodmc.util;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class HttpResponse<T> {
  private final @Nullable T response;

  public HttpResponse(@Nullable T response) {
    this.response = response;
  }

  public @Nullable T getResponse() {
    return this.response;
  }
}
