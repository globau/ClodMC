package au.com.glob.clodmc.modules.bluemap;

import au.com.glob.clodmc.modules.Module;
import de.bluecolored.bluemap.api.BlueMapAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BlueMapAddon {
  protected final @Nullable Class<? extends Module> updater;

  protected BlueMapAddon(@Nullable Class<? extends Module> updater) {
    this.updater = updater;
  }

  protected void onEnable(@NotNull BlueMapAPI api) {}

  protected abstract void onUpdate(@NotNull BlueMapAPI api);
}
