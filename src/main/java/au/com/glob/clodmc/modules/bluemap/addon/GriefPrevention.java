package au.com.glob.clodmc.modules.bluemap.addon;

import au.com.glob.clodmc.modules.bluemap.BlueMapAddon;
import au.com.glob.clodmc.modules.bluemap.BlueMapSource;
import de.bluecolored.bluemap.api.BlueMapAPI;
import org.jetbrains.annotations.NotNull;

public class GriefPrevention extends BlueMapAddon {
  GriefPrevention(@NotNull BlueMapAPI api) {
    super(api, BlueMapSource.GRIEF_PREVENTION, true);
  }

  @Override
  protected void onUpdate() {
    //
  }
}
