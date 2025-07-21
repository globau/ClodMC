package au.com.glob.clodmc.util;

import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NullMarked;

/** Standardised styling for messages sent to players via chat */
@NullMarked
public final class Chat {
  private enum Style {
    FYI("<grey>"),
    WHISPER("<grey><i>"),
    INFO("<yellow>"),
    WARNING("<yellow><i>"),
    ERROR("<red>");

    final String prefix;

    Style(String prefix) {
      this.prefix = prefix;
    }
  }

  private static void sendMessage(CommandSender sender, Style style, String message) {
    sender.sendRichMessage(style.prefix + message);
  }

  public static void fyi(CommandSender sender, String message) {
    sendMessage(sender, Style.FYI, message);
  }

  public static void whisper(CommandSender sender, String message) {
    sendMessage(sender, Style.WHISPER, message);
  }

  public static void info(CommandSender sender, String message) {
    sendMessage(sender, Style.INFO, message);
  }

  public static void warning(CommandSender sender, String message) {
    sendMessage(sender, Style.WARNING, message);
  }

  public static void error(CommandSender sender, String message) {
    sendMessage(sender, Style.ERROR, message);
  }
}
