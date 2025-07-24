package au.com.glob.clodmc.util;

import com.google.gson.JsonObject;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class HttpJsonResponse extends HttpResponse<JsonObject> {
  HttpJsonResponse(@Nullable JsonObject response) {
    super(response);
  }
}
