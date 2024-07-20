package au.com.glob.clodmc.modules.bluemap;

import de.bluecolored.bluemap.api.BlueMapAPI;
import org.jetbrains.annotations.NotNull;

public abstract class BlueMapAddon {
  protected final @NotNull BlueMapAPI api;
  protected final @NotNull BlueMapSource source;
  protected final boolean canUpdate;

  protected BlueMapAddon(
      @NotNull BlueMapAPI api, @NotNull BlueMapSource source, boolean canUpdate) {
    this.api = api;
    this.source = source;
    this.canUpdate = canUpdate;
  }

  protected abstract void onUpdate();
}
