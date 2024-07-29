package au.com.glob.clodmc.modules.bluemap;

import au.com.glob.clodmc.modules.Module;
import de.bluecolored.bluemap.api.BlueMapAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BlueMapAddon {
  protected final @NotNull BlueMapAPI api;
  protected final @Nullable Class<? extends Module> updater;

  protected BlueMapAddon(@NotNull BlueMapAPI api, @Nullable Class<? extends Module> updater) {
    this.api = api;
    this.updater = updater;
  }

  protected abstract void onUpdate();
}
