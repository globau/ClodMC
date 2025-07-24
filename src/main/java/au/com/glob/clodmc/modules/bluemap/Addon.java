package au.com.glob.clodmc.modules.bluemap;

import de.bluecolored.bluemap.api.BlueMapAPI;
import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class Addon {
  protected final BlueMapAPI api;

  protected Addon(BlueMapAPI api) {
    this.api = api;
  }

  protected abstract void update();
}
