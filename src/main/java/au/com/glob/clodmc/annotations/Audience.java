package au.com.glob.clodmc.annotations;

import org.jspecify.annotations.NullMarked;

/** audience type for commands and notifications */
@NullMarked
public enum Audience {
  PLAYER,
  SERVER,
  ADMIN
}
