package au.com.glob.clodmc.modules;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface Module {
  default @Nullable String dependsOn() {
    return null;
  }

  default void initialise() {}

  default void loadConfig() {}
}
