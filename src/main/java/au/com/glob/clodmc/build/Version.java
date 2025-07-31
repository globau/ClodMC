package au.com.glob.clodmc.build;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** outputs a version number derived from the latest commit's timestamp */
@SuppressWarnings("NullabilityAnnotations")
public class Version {
  private static final DateTimeFormatter VERSION_FORMAT =
      DateTimeFormatter.ofPattern("yy.MMdd.HHmm");
  private static final ZoneOffset GMT_PLUS_8 = ZoneOffset.ofHours(8);

  private static String readStream(InputStream stream) throws IOException {
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
      return reader.readLine();
    }
  }

  public static void main(String[] args) {
    try {
      // get commit timestamp using try-with-resources
      ProcessBuilder pb = new ProcessBuilder("git", "show", "--no-patch", "--format=%ct");
      Process process = pb.start();
      String stdout = readStream(process.getInputStream());
      int exitCode = process.waitFor();
      if (exitCode != 0) {
        throw new RuntimeException("git: " + readStream(process.getErrorStream()));
      }

      // parse timestamp and convert to gmt+8
      long timestamp = Long.parseLong(stdout.trim());
      Instant instant = Instant.ofEpochSecond(timestamp);

      // format as version string (date+hh:mm pretending to be a version string)
      String version = instant.atOffset(GMT_PLUS_8).format(VERSION_FORMAT);
      System.out.println(version);

    } catch (Exception e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }
}
