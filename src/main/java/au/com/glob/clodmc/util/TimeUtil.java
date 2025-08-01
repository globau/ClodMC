package au.com.glob.clodmc.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.jspecify.annotations.NullMarked;

/** Date/Time/Timezone helpers */
@NullMarked
public class TimeUtil {
  private static final ZoneId LOCAL_TZ = ZoneId.of("Australia/Perth");
  private static final ZoneId UTC = ZoneId.of("UTC");

  public static LocalDateTime localNow() {
    return LocalDateTime.now(LOCAL_TZ);
  }

  public static ZonedDateTime utcNow() {
    return ZonedDateTime.now(UTC);
  }
}
