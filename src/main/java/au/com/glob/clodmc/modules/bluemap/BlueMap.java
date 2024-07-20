package au.com.glob.clodmc.modules.bluemap;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.bluemap.addon.Anchors;
import au.com.glob.clodmc.modules.bluemap.addon.Spawn;
import au.com.glob.clodmc.modules.bluemap.addon.WorldBorder;
import au.com.glob.clodmc.modules.gateways.Gateways;
import au.com.glob.clodmc.modules.server.CircularWorldBorder;
import de.bluecolored.bluemap.api.BlueMapAPI;
import java.util.ArrayList;
import java.util.List;
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
          this.addons.add(new Anchors(api, ClodMC.getModule(Gateways.class)));
          this.addons.add(new Spawn(api));
          this.addons.add(new WorldBorder(api, ClodMC.getModule(CircularWorldBorder.class)));

          for (BlueMapAddon addon : this.addons) {
            addon.onUpdate();
          }
        });
  }

  @EventHandler
  public void onBlueMapUpdateRequired(@NotNull BlueMapUpdateEvent event) {
    for (BlueMapAddon addon : this.addons) {
      if (addon.source.equals(event.getSource())) {
        if (addon.canUpdate) {
          addon.onUpdate();
        }
        break;
      }
    }
  }
}
