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

/** http client helpers */
@NullMarked
public final class HttpClient {
  private HttpClient() {}

  private static final String USER_AGENT = "glob.au/clod-mc";
  private static final int CONNECT_TIMEOUT_MS = 3000;
  private static final int READ_TIMEOUT_MS = 5000;

  private static final Gson gson = new Gson();

  // perform get request and parse json response
  public static HttpJsonResponse getJSON(
      final String urlString, final Map<String, String> headers) {
    return readJsonResponse(request(urlString, headers));
  }

  // create and configure http connection
  private static HttpURLConnection request(
      final String urlString, @Nullable final Map<String, String> headers) {
    final HttpURLConnection connection;

    try {
      final URL url = URI.create(urlString).toURL();
      connection = (HttpURLConnection) url.openConnection();
    } catch (final Exception exception) {
      throw new RuntimeException("Failed to create connection", exception);
    }

    try {
      connection.setRequestMethod("GET");
      connection.setUseCaches(false);
      connection.setRequestProperty("User-Agent", USER_AGENT);
      connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
      connection.setReadTimeout(READ_TIMEOUT_MS);
      if (headers != null) {
        for (final Map.Entry<String, String> entry : headers.entrySet()) {
          connection.setRequestProperty(entry.getKey(), entry.getValue());
        }
      }
    } catch (final Exception exception) {
      throw new RuntimeException("Failed to create request", exception);
    }

    return connection;
  }

  // read and parse json from http response
  private static HttpJsonResponse readJsonResponse(final HttpURLConnection connection) {
    final InputStreamReader streamReader = createReader(connection);
    if (streamReader == null) {
      return new HttpJsonResponse(null);
    }
    try (streamReader) {
      final JsonObject response = gson.fromJson(streamReader, JsonObject.class);
      return new HttpJsonResponse(response);
    } catch (final Exception exception) {
      throw new RuntimeException("Failed to read response", exception);
    }
  }

  // create stream reader from connection input or error stream
  private static @Nullable InputStreamReader createReader(final HttpURLConnection connection) {
    InputStream stream;
    try {
      stream = connection.getInputStream();
    } catch (final Exception exception) {
      stream = connection.getErrorStream();
    }
    if (stream != null) {
      return new InputStreamReader(stream, StandardCharsets.UTF_8);
    }
    return null;
  }
}
