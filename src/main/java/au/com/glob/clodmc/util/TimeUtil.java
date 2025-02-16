package au.com.glob.clodmc.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.jetbrains.annotations.NotNull;

/** Date/Time/Timezone helpers */
public class TimeUtil {
  private static final @NotNull ZoneId LOCAL_TZ = ZoneId.of("Australia/Perth");
  private static final @NotNull ZoneId UTC = ZoneId.of("UTC");

  public static @NotNull LocalDateTime localNow() {
    return LocalDateTime.now(LOCAL_TZ);
  }

  public static @NotNull ZonedDateTime utcNow() {
    return ZonedDateTime.now(UTC);
  }
}
