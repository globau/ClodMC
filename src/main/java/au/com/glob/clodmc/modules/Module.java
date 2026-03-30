package au.com.glob.clodmc.modules;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.util.Logger;
import java.util.function.Supplier;
import org.bukkit.event.Listener;
import org.jspecify.annotations.NullMarked;

/** base class for all ClodMC plugin modules */
@NullMarked
public abstract class Module {
  protected Module(final String... requiredPlugins) {
    for (final String plugin : requiredPlugins) {
      if (!ClodMC.isPluginEnabled(plugin)) {
        Logger.warning(
            "Cannot load module %s: depends on plugin %s which is not enabled"
                .formatted(this.getClass().getSimpleName(), plugin));
        throw new ModuleSkippedException();
      }
    }
    if (this instanceof final Listener listener) {
      ClodMC.registerListener(listener);
    }
  }

  public static void create(final Supplier<Module> supplier) {
    try {
      final Module _ = supplier.get();
    } catch (final ModuleSkippedException _) {
      // module skipped due to missing plugin dependency
    }
  }
}
