package au.com.glob.clodmc.util;

import au.com.glob.clodmc.modules.BlueMapModule;
import de.bluecolored.bluemap.api.BlueMapAPI;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class BlueMap {
  public static BlueMapAPI api;

  public static void onEnable(@NotNull List<BlueMapModule> modules) {
    BlueMapAPI.onEnable(
        (BlueMapAPI blueMapAPI) -> {
          api = blueMapAPI;
          for (BlueMapModule module : modules) {
            module.onBlueMapEnable();
          }
        });
  }
}
