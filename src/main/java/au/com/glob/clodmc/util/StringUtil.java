package au.com.glob.clodmc.util;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.Translatable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** String helpers */
@NullMarked
public class StringUtil {
  public static String relativeTime(long seconds) {
    long dd = TimeUnit.SECONDS.toDays(seconds);
    long hh = TimeUnit.SECONDS.toHours(seconds) % 24;
    long mm = TimeUnit.SECONDS.toMinutes(seconds) % 60;
    seconds %= 60;

    if (dd >= 7) {
      return plural(dd, "day");
    } else if (dd > 0) {
      return plural(dd, "day") + " " + plural(hh, "hour");
    } else if (hh > 0) {
      return plural(hh, "hour") + " " + plural(mm, "minute");
    } else if (mm > 0) {
      return plural(mm, "minute") + " " + plural(seconds, "second");
    } else {
      return plural(seconds, "second");
    }
  }

  public static String plural(long value, String unit) {
    return String.format("%d %s%s", value, unit, value == 1 ? "" : "s");
  }

  public static String plural2(long value, String unit) {
    return value == 1 ? unit : String.format("%d %ss", value, unit);
  }

  public static String joinComma(List<String> items) {
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

  public static String toTitleCase(String value) {
    StringBuilder titleCase = new StringBuilder();
    for (String word : value.split("\\s+", -1)) {
      titleCase
          .append(Character.toUpperCase(word.charAt(0)))
          .append(word.substring(1).toLowerCase(Locale.ENGLISH))
          .append(" ");
    }
    return titleCase.toString().trim();
  }

  public static String asText(Translatable component) {
    return asText(
        GlobalTranslator.render(
            Component.translatable(component.translationKey()), Locale.ENGLISH));
  }

  public static String asText(Component component) {
    return PlainTextComponentSerializer.plainText().serialize(component);
  }

  public static String asText(@Nullable Component component, String fallback) {
    return component == null ? fallback : asText(component);
  }

  public static Component asComponent(String value) {
    return MiniMessage.miniMessage().deserialize(value);
  }
}
