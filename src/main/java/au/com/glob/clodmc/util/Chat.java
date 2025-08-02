package au.com.glob.clodmc.util;

import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NullMarked;

/** Standardised styling for messages sent to players via chat */
@NullMarked
public final class Chat {
  private static void sendMessage(CommandSender sender, ChatStyle style, String message) {
    sender.sendRichMessage(style.prefix + message);
  }

  public static void fyi(CommandSender sender, String message) {
    sendMessage(sender, ChatStyle.FYI, message);
  }

  public static void whisper(CommandSender sender, String message) {
    sendMessage(sender, ChatStyle.WHISPER, message);
  }

  public static void plain(CommandSender sender, String message) {
    sendMessage(sender, ChatStyle.PLAIN, message);
  }

  public static void info(CommandSender sender, String message) {
    sendMessage(sender, ChatStyle.INFO, message);
  }

  public static void warning(CommandSender sender, String message) {
    sendMessage(sender, ChatStyle.WARNING, message);
  }

  public static void error(CommandSender sender, String message) {
    sendMessage(sender, ChatStyle.ERROR, message);
  }
}
