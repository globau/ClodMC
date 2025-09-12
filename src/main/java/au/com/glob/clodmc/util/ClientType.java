package au.com.glob.clodmc.util;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** enum for minecraft client types (java vs bedrock) */
@NullMarked
public enum ClientType {
  JAVA("java"),
  BEDROCK("bedrock");

  private final String name;

  ClientType(final String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return this.name;
  }

  // parse client type from string name
  public static @Nullable ClientType of(@Nullable final String name) {
    if (name == null) {
      return null;
    }
    for (final ClientType gameType : ClientType.values()) {
      if (gameType.name.equalsIgnoreCase(name)) {
        return gameType;
      }
    }
    return null;
  }
}
