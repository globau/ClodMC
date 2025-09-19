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

/** string helpers */
@NullMarked
public final class StringUtil {
  // format seconds as human-readable relative time
  public static String relativeTime(final long inputSeconds) {
    final long dd = TimeUnit.SECONDS.toDays(inputSeconds);
    final long hh = TimeUnit.SECONDS.toHours(inputSeconds) % 24;
    final long mm = TimeUnit.SECONDS.toMinutes(inputSeconds) % 60;
    final long seconds = inputSeconds % 60;

    if (dd >= 7) {
      return plural(dd, "day");
    } else if (dd > 0) {
      return "%s %s".formatted(plural(dd, "day"), plural(hh, "hour"));
    } else if (hh > 0) {
      return "%s %s".formatted(plural(hh, "hour"), plural(mm, "minute"));
    } else if (mm > 0) {
      return "%s %s".formatted(plural(mm, "minute"), plural(seconds, "second"));
    } else {
      return plural(seconds, "second");
    }
  }

  // format value with unit, adding 's' for plural
  public static String plural(final long value, final String unit) {
    return String.format("%d %s%s", value, unit, value == 1 ? "" : "s");
  }

  // alternative plural formatting with value prefix
  public static String plural2(final long value, final String unit) {
    return value == 1 ? unit : String.format("%d %ss", value, unit);
  }

  // join list with oxford commas and 'and' for last item
  public static String joinComma(final List<String> items) {
    if (items.isEmpty()) {
      return "";
    }
    if (items.size() == 1) {
      return items.getFirst();
    }
    if (items.size() == 2) {
      return String.join(" and ", items);
    }
    return "%s, and %s"
        .formatted(String.join(", ", items.subList(0, items.size() - 1)), items.getLast());
  }

  // convert string to title case
  public static String toTitleCase(final String value) {
    final StringBuilder titleCase = new StringBuilder();
    for (final String word : value.split("\\s+", -1)) {
      titleCase
          .append(Character.toUpperCase(word.charAt(0)))
          .append(word.substring(1).toLowerCase(Locale.ENGLISH))
          .append(" ");
    }
    return titleCase.toString().trim();
  }

  // convert translatable component to plain text
  public static String asText(final Translatable component) {
    return asText(
        GlobalTranslator.render(
            Component.translatable(component.translationKey()), Locale.ENGLISH));
  }

  // convert adventure component to plain text
  public static String asText(final Component component) {
    return PlainTextComponentSerializer.plainText().serialize(component);
  }

  // convert component to text with fallback
  public static String asText(@Nullable final Component component, final String fallback) {
    return component == null ? fallback : asText(component);
  }

  // parse minimessage string into component
  public static Component asComponent(final String value) {
    return MiniMessage.miniMessage().deserialize(value);
  }
}
