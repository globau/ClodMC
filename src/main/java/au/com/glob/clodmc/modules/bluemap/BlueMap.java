package au.com.glob.clodmc.modules.bluemap;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.bluemap.addon.GatewaysAddon;
import au.com.glob.clodmc.modules.bluemap.addon.GriefPreventionAddon;
import au.com.glob.clodmc.modules.bluemap.addon.SpawnAddon;
import au.com.glob.clodmc.modules.bluemap.addon.WorldBorderAddon;
import au.com.glob.clodmc.modules.interactions.Gateways;
import au.com.glob.clodmc.util.Logger;
import au.com.glob.clodmc.util.Schedule;
import de.bluecolored.bluemap.api.BlueMapAPI;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Bridge between ClodMC modules and BlueMap */
public class BlueMap implements Module, Listener {
  private final @NotNull List<BlueMapAddon> addons = new ArrayList<>(4);
  private @Nullable BlueMapAPI api;

  @Override
  public String dependsOn() {
    return "BlueMap";
  }

  @Override
  public void initialise() {
    this.addons.add(new GatewaysAddon(ClodMC.getModule(Gateways.class)));
    this.addons.add(new SpawnAddon());
    this.addons.add(new WorldBorderAddon());
    if (Bukkit.getPluginManager().isPluginEnabled("GriefPrevention")) {
      this.addons.add(new GriefPreventionAddon());
    }
  }

  @Override
  public void loadConfig() {
    BlueMapAPI.onEnable(
        (BlueMapAPI api) -> {
          Logger.info("Initialising BlueMap addons");
          this.api = api;

          for (BlueMapAddon addon : this.addons) {
            try {
              addon.onEnable(api);
            } catch (Exception e) {
              Logger.exception(e);
            }
          }

          // delayed to avoid issue where bluemap was ignoring some addons
          Schedule.delayed(
              5 * 20,
              () -> {
                if (this.api == null) {
                  return;
                }
                Logger.info("Triggering BlueMap addon updates");
                for (BlueMapAddon addon : this.addons) {
                  try {
                    addon.onUpdate(this.api);
                  } catch (Exception e) {
                    Logger.exception(e);
                  }
                }
              });
        });
  }

  @EventHandler
  public void onBlueMapUpdate(@NotNull BlueMapUpdateEvent event) {
    if (this.api == null) {
      return;
    }

    for (BlueMapAddon addon : this.addons) {
      if (event.getSender().equals(addon.updater)) {
        try {
          addon.onUpdate(this.api);
        } catch (Exception e) {
          Logger.exception(e);
        }
        break;
      }
    }
  }
}
