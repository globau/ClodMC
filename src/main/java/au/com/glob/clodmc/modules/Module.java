package au.com.glob.clodmc.modules;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Base interface for all ClodMC plugin modules.
 *
 * <p>Modules are registered by and must be added to {@link ModuleRegistry}
 */
@NullMarked
public interface Module {
  /**
   * specifies a plugin which the module requires to be loaded (eg. GriefPrevention). if the plugin
   * is not loaded, the module will not be loaded.
   *
   * @return the name of the plugin this depends on, or null if no dependency
   */
  default @Nullable String dependsOn() {
    return null;
  }

  /**
   * called during module initialisation phase.
   *
   * <p>use this to perform initialisation instead of the constructor
   */
  default void initialise() {}

  /**
   * called during config loading phase after all modules are initialised.
   *
   * <p>use this to load configuration data that may depend on other modules being available.
   */
  default void loadConfig() {}
}
