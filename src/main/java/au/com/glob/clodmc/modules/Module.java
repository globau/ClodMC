package au.com.glob.clodmc.modules;

import org.jspecify.annotations.NullMarked;

/**
 * Base interface for all ClodMC plugin modules.
 *
 * <p>Modules are registered by and must be added to {@link ModuleRegistry}
 */
@NullMarked
public interface Module {
  /**
   * called during config loading phase after all modules are initialised.
   *
   * <p>use this to load configuration data that may depend on other modules being available.
   */
  default void loadConfig() {}
}
