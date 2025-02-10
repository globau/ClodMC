package au.com.glob.clodmc.modules.bluemap;

import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.interactions.Gateways;
import au.com.glob.clodmc.util.Logger;
import au.com.glob.clodmc.util.Schedule;
import de.bluecolored.bluemap.api.BlueMapAPI;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Bridge between ClodMC modules and BlueMap */
public class BlueMap implements Module, Listener {
  private final @NotNull List<Addon> addons = new ArrayList<>(4);

  @Override
  public String dependsOn() {
    return "BlueMap";
  }

  private void register(@NotNull Class<? extends Addon> cls, @Nullable BlueMapAPI api) {
    try {
      this.addons.add(cls.getDeclaredConstructor(BlueMapAPI.class).newInstance(api));
    } catch (Exception e) {
      Logger.exception(e);
    }
  }

  @Override
  public void loadConfig() {
    BlueMapAPI.onEnable(
        (BlueMapAPI api) -> {
          Logger.info("Initialising BlueMap addons");
          this.register(BlueMapSpawn.class, api);
          this.register(BlueMapWorldBorder.class, api);
          this.register(Gateways.BlueMapGateways.class, api);
          if (Bukkit.getPluginManager().isPluginEnabled("GriefPrevention")) {
            this.register(BlueMapGriefPrevention.class, api);
          }

          // delayed to avoid issue where bluemap was ignoring some addons
          Schedule.delayed(
              5 * 20,
              () -> {
                Logger.info("Triggering BlueMap addon updates");
                for (Addon addon : this.addons) {
                  try {
                    addon.update();
                  } catch (Exception e) {
                    Logger.exception(e);
                  }
                }
              });
        });
  }

  public abstract static class Addon {
    protected @Nullable BlueMapAPI api;

    protected Addon(@NotNull BlueMapAPI api) {
      this.api = api;
    }

    protected abstract void update();
  }
}
