package au.com.glob.clodmc.util;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/** Standardised styling for messages sent to players via the action bar */
@NullMarked
public final class ActionBar {
  private static void setActionBar(Player player, ChatStyle style, String message) {
    player.sendActionBar(StringUtil.asComponent("%s%s".formatted(style, message)));
  }

  public static void info(Player player, String message) {
    setActionBar(player, ChatStyle.INFO, message);
  }

  public static void info(Player player, Component message) {
    info(player, StringUtil.asText(message));
  }

  public static void plain(Player player, String message) {
    setActionBar(player, ChatStyle.PLAIN, message);
  }
}
