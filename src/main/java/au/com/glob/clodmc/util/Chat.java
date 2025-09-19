package au.com.glob.clodmc.util;

import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NullMarked;

/** standardised styling for messages sent to players via chat */
@NullMarked
public final class Chat {
  // send styled message to command sender
  private static void sendMessage(
      final CommandSender sender, final ChatStyle style, final String message) {
    sender.sendRichMessage("%s%s".formatted(style.prefix, message));
  }

  // send fyi message
  public static void fyi(final CommandSender sender, final String message) {
    sendMessage(sender, ChatStyle.FYI, message);
  }

  // send whisper message
  public static void whisper(final CommandSender sender, final String message) {
    sendMessage(sender, ChatStyle.WHISPER, message);
  }

  // send plain message
  public static void plain(final CommandSender sender, final String message) {
    sendMessage(sender, ChatStyle.PLAIN, message);
  }

  // send info message
  public static void info(final CommandSender sender, final String message) {
    sendMessage(sender, ChatStyle.INFO, message);
  }

  // send warning message
  public static void warning(final CommandSender sender, final String message) {
    sendMessage(sender, ChatStyle.WARNING, message);
  }

  // send error message
  public static void error(final CommandSender sender, final String message) {
    sendMessage(sender, ChatStyle.ERROR, message);
  }
}
