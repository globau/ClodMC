package au.com.glob.clodmc.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** HTTP client helpers */
public class HttpClient {
  private HttpClient() {}

  private static final @NotNull String USER_AGENT = "glob.au/clod-mc";

  private static final @NotNull Gson gson = new Gson();

  @NotNull public static JsonHttpResponse getJSON(@NotNull String urlString) {
    return readJsonResponse(request(urlString, null));
  }

  @NotNull public static JsonHttpResponse getJSON(
      @NotNull String urlString, @NotNull Map<String, String> headers) {
    return readJsonResponse(request(urlString, headers));
  }

  @NotNull private static HttpURLConnection request(
      @NotNull String urlString, @Nullable Map<String, String> headers) {
    HttpURLConnection connection;

    try {
      URL url = URI.create(urlString).toURL();
      connection = (HttpURLConnection) url.openConnection();
    } catch (Exception exception) {
      throw new RuntimeException("Failed to create connection", exception);
    }

    try {
      connection.setRequestMethod("GET");
      connection.setUseCaches(false);
      connection.setRequestProperty("User-Agent", USER_AGENT);
      connection.setConnectTimeout(3000);
      connection.setReadTimeout(5000);
      if (headers != null) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
          connection.setRequestProperty(entry.getKey(), entry.getValue());
        }
      }
    } catch (Exception exception) {
      throw new RuntimeException("Failed to create request", exception);
    }

    return connection;
  }

  private static @NotNull JsonHttpResponse readJsonResponse(@NotNull HttpURLConnection connection) {
    InputStreamReader streamReader = createReader(connection);
    try (streamReader) {
      if (streamReader == null) {
        return new JsonHttpResponse(null);
      }
      JsonObject response = gson.fromJson(streamReader, JsonObject.class);
      return new JsonHttpResponse(response);
    } catch (Exception exception) {
      throw new RuntimeException("Failed to read response", exception);
    }
  }

  private static @Nullable InputStreamReader createReader(@NotNull HttpURLConnection connection) {
    InputStream stream;
    try {
      stream = connection.getInputStream();
    } catch (Exception exception) {
      stream = connection.getErrorStream();
    }
    if (stream != null) {
      return new InputStreamReader(stream, StandardCharsets.UTF_8);
    }
    return null;
  }

  public static class HttpResponse<T> {
    private final @Nullable T response;

    public HttpResponse(@Nullable T response) {
      this.response = response;
    }

    public @Nullable T getResponse() {
      return this.response;
    }
  }

  public static final class JsonHttpResponse extends HttpResponse<JsonObject> {
    JsonHttpResponse(@Nullable JsonObject response) {
      super(response);
    }
  }
}
