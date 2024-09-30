package au.com.glob.clodmc.modules.bluemap;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.bluemap.addon.CircularWorldBorderAddon;
import au.com.glob.clodmc.modules.bluemap.addon.GatewaysAddon;
import au.com.glob.clodmc.modules.bluemap.addon.GriefPreventionAddon;
import au.com.glob.clodmc.modules.bluemap.addon.SpawnAddon;
import au.com.glob.clodmc.modules.gateways.Gateways;
import au.com.glob.clodmc.modules.server.CircularWorldBorder;
import au.com.glob.clodmc.util.Logger;
import de.bluecolored.bluemap.api.BlueMapAPI;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/** Bridge between ClodMC modules and BlueMap */
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
              Logger.exception(e);
            }
          }
        });
  }

  @EventHandler
  public void onBlueMapUpdate(@NotNull BlueMapUpdateEvent event) {
    for (BlueMapAddon addon : this.addons) {
      if (event.getSender().equals(addon.updater)) {
        try {
          addon.onUpdate();
        } catch (Exception e) {
          Logger.exception(e);
        }
        break;
      }
    }
  }
}
