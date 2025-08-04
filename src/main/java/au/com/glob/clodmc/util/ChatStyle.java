package au.com.glob.clodmc.util;

import org.jspecify.annotations.NullMarked;

/** chat message formatting styles with colour prefixes */
@NullMarked
public enum ChatStyle {
  FYI("<grey>"),
  WHISPER("<grey><i>"),
  PLAIN(""),
  INFO("<yellow>"),
  WARNING("<yellow><i>"),
  ERROR("<red>");

  final String prefix;

  ChatStyle(String prefix) {
    this.prefix = prefix;
  }
}
