package au.com.glob.clodmc.util;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/** Standardised styling for messages sent to players via the action bar */
@NullMarked
public final class ActionBar {
  private static void setActionBar(
      final Player player, final ChatStyle style, final String message) {
    player.sendActionBar(StringUtil.asComponent("%s%s".formatted(style.prefix, message)));
  }

  public static void info(final Player player, final String message) {
    setActionBar(player, ChatStyle.INFO, message);
  }

  public static void info(final Player player, final Component message) {
    info(player, StringUtil.asText(message));
  }

  public static void plain(final Player player, final String message) {
    setActionBar(player, ChatStyle.PLAIN, message);
  }
}
