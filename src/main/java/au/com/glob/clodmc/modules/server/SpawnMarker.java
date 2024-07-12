package au.com.glob.clodmc.modules.server;

import au.com.glob.clodmc.modules.BlueMapModule;
import au.com.glob.clodmc.modules.Module;

public class SpawnMarker implements Module, BlueMapModule {
  @Override
  public void onBlueMapEnable() {
    new SpawnMarkerBlueMap();
  }
}
