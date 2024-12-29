package au.com.glob.clodmc.util;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.Translatable;
import org.jetbrains.annotations.NotNull;

/** String helpers */
public class StringUtil {
  public static @NotNull String relativeTime(long seconds) {
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

  public static @NotNull String plural(long value, @NotNull String unit) {
    return String.format("%d %s%s", value, unit, value == 1 ? "" : "s");
  }

  public static @NotNull String plural2(long value, @NotNull String unit) {
    return value == 1 ? unit : String.format("%d %ss", value, unit);
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

  public static @NotNull String toTitleCase(@NotNull String value) {
    StringBuilder titleCase = new StringBuilder();
    for (String word : value.split("\\s+")) {
      titleCase
          .append(Character.toUpperCase(word.charAt(0)))
          .append(word.substring(1).toLowerCase())
          .append(" ");
    }
    return titleCase.toString().trim();
  }

  public static @NotNull String asText(@NotNull Translatable component) {
    return asText(
        GlobalTranslator.render(
            Component.translatable(component.translationKey()), Locale.ENGLISH));
  }

  public static @NotNull String asText(@NotNull Component component) {
    return PlainTextComponentSerializer.plainText().serialize(component);
  }

  public static @NotNull Component asComponent(@NotNull String value) {
    return MiniMessage.miniMessage().deserialize(value);
  }
}
