package au.com.glob.clodmc.util;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;

public class MiscUtil {
  public static @NotNull String relativeTime(long seconds) {
    long ss = Math.round(seconds);
    long dd = TimeUnit.SECONDS.toDays(ss);
    long hh = TimeUnit.SECONDS.toHours(ss) % 24;
    long mm = TimeUnit.SECONDS.toMinutes(ss) % 60;
    ss %= 60;

    if (dd >= 7) {
      return plural(dd, "day");
    } else if (dd > 0) {
      return plural(dd, "day") + " " + plural(hh, "hour");
    } else if (hh > 0) {
      return plural(hh, "hour") + " " + plural(mm, "minute");
    } else if (mm > 0) {
      return plural(mm, "minute") + " " + plural(ss, "second");
    } else {
      return plural(ss, "second");
    }
  }

  public static @NotNull String plural(long value, @NotNull String unit) {
    return String.format("%d %s%s", value, unit, value == 1 ? "" : "s");
  }

  public static @NotNull String joinComma(@NotNull List<String> items) {
    if (items.isEmpty()) {
      return "";
    }
    if (items.size() == 1) {
      return items.getFirst();
    }
    if (items.size() == 2) {
      return String.join(" and ", items);
    }
    return String.join(", ", items.subList(0, items.size() - 1)) + ", and " + items.getLast();
  }
}
