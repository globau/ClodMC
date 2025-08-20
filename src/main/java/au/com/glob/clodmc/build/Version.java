package au.com.glob.clodmc.build;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** outputs a version number derived from the latest commit's timestamp */
@SuppressWarnings("NullabilityAnnotations")
public class Version {
  // execute command and return stdout output
  private static String capture(String... command) throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder(command);
    Process process = pb.start();
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    int exitCode = process.waitFor();
    if (exitCode != 0) {
      throw new RuntimeException(
          new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
    }
    return output;
  }

  // generate version string from git commit timestamp
  public static void main(String[] args) {
    try {
      // parse commit timestamp and convert to gmt+8
      String commitTimestamp = capture("git", "show", "--no-patch", "--format=%ct");
      long timestamp = Long.parseLong(commitTimestamp.trim());
      OffsetDateTime datetime = Instant.ofEpochSecond(timestamp).atOffset(ZoneOffset.ofHours(8));

      // format as version string (date+hh:mm pretending to be a version string)
      int year = datetime.getYear() % 100;
      int monthDay = datetime.getMonthValue() * 100 + datetime.getDayOfMonth();
      int hourMin = datetime.getHour() * 100 + datetime.getMinute();
      System.out.printf("%d.%d.%d%n", year, monthDay, hourMin);
    } catch (Exception e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }
}
