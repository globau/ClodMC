package au.com.glob.clodmc.util;

import java.util.List;
import org.jetbrains.annotations.NotNull;

public class MiscUtil {
  public static @NotNull String relativeTime(long ss) {
    long mm = Math.round(ss / 60.0f);
    long hh = Math.round(mm / 60.0f);
    long dd = Math.round(hh / 24.0f);
    long mo = Math.round(dd / 30.0f);
    long yy = Math.round(mo / 12.0f);

    if (ss < 10) {
      return "now";
    } else if (ss < 45) {
      return ss + "s";
    } else if (ss < 90) {
      return "1m";
    } else if (mm < 45) {
      return mm + "m";
    } else if (mm < 90) {
      return "1h";
    } else if (hh < 24) {
      return hh + "h";
    } else if (hh < 36) {
      return "1d";
    } else if (dd < 30) {
      return dd + "d";
    } else if (dd < 45) {
      return "1mn";
    } else if (mo < 12) {
      return mo + "mn";
    } else if (mo < 18) {
      return "1y";
    } else {
      return yy + "y";
    }
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
