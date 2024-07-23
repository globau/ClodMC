package au.com.glob.clodmc.modules.bluemap;

import de.bluecolored.bluemap.api.BlueMapAPI;
import org.jetbrains.annotations.NotNull;

public abstract class BlueMapAddon {
  protected final @NotNull BlueMapAPI api;

  protected BlueMapAddon(@NotNull BlueMapAPI api) {
    this.api = api;
  }

  protected abstract void updateMarkers();
}
