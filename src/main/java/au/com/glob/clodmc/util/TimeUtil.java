package au.com.glob.clodmc.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import org.jetbrains.annotations.NotNull;

/** Date/Time/Timezone helpers */
public class TimeUtil {
  private static final @NotNull ZoneId TIME_ZONE = ZoneId.of("Australia/Perth");

  public static @NotNull LocalDateTime now() {
    return LocalDateTime.now(TIME_ZONE);
  }
}
