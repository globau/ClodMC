package au.com.glob.clodmc.util;

import org.jspecify.annotations.NullMarked;

@NullMarked
public enum ChatStyle {
  FYI("<grey>"),
  WHISPER("<grey><i>"),
  INFO("<yellow>"),
  WARNING("<yellow><i>"),
  ERROR("<red>");

  final String prefix;

  ChatStyle(String prefix) {
    this.prefix = prefix;
  }
}
