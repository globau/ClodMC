package au.com.glob.clodmc.modules.bluemap;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.bluemap.addon.CircularWorldBorderAddon;
import au.com.glob.clodmc.modules.bluemap.addon.GatewaysAddon;
import au.com.glob.clodmc.modules.bluemap.addon.GriefPreventionAddon;
import au.com.glob.clodmc.modules.bluemap.addon.SpawnAddon;
import au.com.glob.clodmc.modules.gateways.Gateways;
import au.com.glob.clodmc.modules.server.CircularWorldBorder;
import de.bluecolored.bluemap.api.BlueMapAPI;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class BlueMap implements Module, Listener {
  private final @NotNull List<BlueMapAddon> addons = new ArrayList<>(3);

  @Override
  public String dependsOn() {
    return "BlueMap";
  }

  @Override
  public void loadConfig() {
    BlueMapAPI.onEnable(
        (BlueMapAPI api) -> {
          this.addons.add(
              new CircularWorldBorderAddon(api, ClodMC.getModule(CircularWorldBorder.class)));
          this.addons.add(new GatewaysAddon(api, ClodMC.getModule(Gateways.class)));
          this.addons.add(new SpawnAddon(api));

          if (Bukkit.getPluginManager().isPluginEnabled("GriefPrevention")) {
            this.addons.add(new GriefPreventionAddon(api));
          }

          for (BlueMapAddon addon : this.addons) {
            try {
              addon.onUpdate();
            } catch (Exception e) {
              ClodMC.logException(e);
            }
          }
        });
  }

  @EventHandler
  public void onBlueMapUpdateRequired(@NotNull BlueMapUpdateEvent event) {
    for (BlueMapAddon addon : this.addons) {
      if (addon.source.equals(event.getSource())) {
        if (addon.canUpdate) {
          try {
            addon.onUpdate();
          } catch (Exception e) {
            ClodMC.logException(e);
          }
        }
        break;
      }
    }
  }
}
