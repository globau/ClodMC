package au.com.glob.clodmc.build;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** outputs a version number derived from the latest commit's timestamp */
@SuppressWarnings("NullabilityAnnotations")
public final class Version {
  // generate version string from git commit timestamp
  public static void main(final String[] args) {
    Util.mainWrapper(
        () -> {
          // parse commit timestamp and convert to gmt+8
          final String commitTimestamp = Util.capture("git", "show", "--no-patch", "--format=%ct");
          final long timestamp = Long.parseLong(commitTimestamp.trim());
          final OffsetDateTime datetime =
              Instant.ofEpochSecond(timestamp).atOffset(ZoneOffset.ofHours(8));

          // format as version string (date+hh:mm pretending to be a version final string)
          final int year = datetime.getYear() % 100;
          final int monthDay = datetime.getMonthValue() * 100 + datetime.getDayOfMonth();
          final int hourMin = datetime.getHour() * 100 + datetime.getMinute();
          System.out.printf("%d.%d.%d%n", year, monthDay, hourMin);
        });
  }
}
