package au.com.glob.clodmc.modules.bluemap;

import de.bluecolored.bluemap.api.BlueMapAPI;
import org.jspecify.annotations.NullMarked;

/** base class for bluemap addon integrations */
@NullMarked
public abstract class Addon {
  protected final BlueMapAPI api;

  // initialise addon with bluemap api reference
  protected Addon(BlueMapAPI api) {
    this.api = api;
  }

  // update the addon's markers and data on the map
  protected abstract void update();
}
