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
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** HTTP client helpers */
@NullMarked
public class HttpClient {
  private HttpClient() {}

  private static final String USER_AGENT = "glob.au/clod-mc";

  private static final Gson gson = new Gson();

  public static HttpJsonResponse getJSON(String urlString, Map<String, String> headers) {
    return readJsonResponse(request(urlString, headers));
  }

  private static HttpURLConnection request(
      String urlString, @Nullable Map<String, String> headers) {
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

  private static HttpJsonResponse readJsonResponse(HttpURLConnection connection) {
    InputStreamReader streamReader = createReader(connection);
    if (streamReader == null) {
      return new HttpJsonResponse(null);
    }
    try (streamReader) {
      JsonObject response = gson.fromJson(streamReader, JsonObject.class);
      return new HttpJsonResponse(response);
    } catch (Exception exception) {
      throw new RuntimeException("Failed to read response", exception);
    }
  }

  private static @Nullable InputStreamReader createReader(HttpURLConnection connection) {
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
}
