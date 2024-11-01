package au.com.glob.clodmc.modules;

import org.jetbrains.annotations.Nullable;

public interface Module {
  default @Nullable String dependsOn() {
    return null;
  }

  default void initialise() {}

  default void loadConfig() {}
}
