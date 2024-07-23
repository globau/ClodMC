package au.com.glob.clodmc.modules.bluemap;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.bluemap.addon.CircularWorldBorderAddon;
import au.com.glob.clodmc.modules.bluemap.addon.GatewaysAddon;
import au.com.glob.clodmc.modules.bluemap.addon.GriefPreventionAddon;
import au.com.glob.clodmc.modules.bluemap.addon.SpawnAddon;
import de.bluecolored.bluemap.api.BlueMapAPI;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public class BlueMap implements Module {
  private final @NotNull List<BlueMapAddon> addons = new ArrayList<>(3);

  @Override
  public String dependsOn() {
    return "BlueMap";
  }

  @Override
  public void loadConfig() {
    BlueMapAPI.onEnable(
        (BlueMapAPI api) -> {
          this.addons.add(new CircularWorldBorderAddon(api));
          this.addons.add(new GatewaysAddon(api));
          this.addons.add(new SpawnAddon(api));

          if (Bukkit.getPluginManager().isPluginEnabled("GriefPrevention")) {
            this.addons.add(new GriefPreventionAddon(api));
          }

          for (BlueMapAddon addon : this.addons) {
            try {
              addon.updateMarkers();
            } catch (Exception e) {
              ClodMC.logException(e);
            }
          }
        });
  }
}
