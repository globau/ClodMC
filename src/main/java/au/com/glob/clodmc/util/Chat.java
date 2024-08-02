package au.com.glob.clodmc.util;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/** Standardised styling for messages sent to players via chat */
public final class Chat {
  private enum Style {
    FYI("<grey>"),
    WHISPER("<grey><i>"),
    INFO("<yellow>"),
    WARNING("<yellow><i>"),
    ERROR("<red>");

    final @NotNull String prefix;

    Style(@NotNull String prefix) {
      this.prefix = prefix;
    }
  }

  private static void sendMessage(
      @NotNull CommandSender sender, @NotNull Style style, @NotNull String message) {
    sender.sendRichMessage(style.prefix + message);
  }

  public static void fyi(@NotNull CommandSender sender, @NotNull String message) {
    sendMessage(sender, Style.FYI, message);
  }

  public static void whisper(@NotNull CommandSender sender, @NotNull String message) {
    sendMessage(sender, Style.WHISPER, message);
  }

  public static void info(@NotNull CommandSender sender, @NotNull String message) {
    sendMessage(sender, Style.INFO, message);
  }

  public static void warning(@NotNull CommandSender sender, @NotNull String message) {
    sendMessage(sender, Style.WARNING, message);
  }

  public static void error(@NotNull CommandSender sender, @NotNull String message) {
    sendMessage(sender, Style.ERROR, message);
  }
}
