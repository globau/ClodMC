package au.com.glob.clodmc;

import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.ModuleRegistry;
import au.com.glob.clodmc.util.ConfigUtil;
import au.com.glob.clodmc.util.Logger;
import au.com.glob.clodmc.util.Players;
import java.io.File;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import vendored.com.jeff_media.customblockdata.CustomBlockData;

public final class ClodMC extends JavaPlugin implements Listener {
  @SuppressWarnings({"NotNullFieldNotInitialized", "NullAway.Init"})
  public static @NotNull ClodMC instance;

  private final @NotNull ModuleRegistry moduleRegistry = new ModuleRegistry();

  public ClodMC() {
    super();
    instance = this;
  }

  @Override
  public void onLoad() {
    File dataFolder = this.getDataFolder();
    if (!dataFolder.exists() && !dataFolder.mkdirs()) {
      Logger.warning("failed to create " + dataFolder);
    }
  }

  @Override
  public void onEnable() {
    Bukkit.getPluginManager().registerEvents(this, this);
    CustomBlockData.registerListener(this);
    this.moduleRegistry.registerAll();

    // ensure all configs can be deserialised, halt server if not to avoid dataloss
    try {
      ConfigUtil.sanityCheckConfigs();

      for (Module module : this.moduleRegistry) {
        module.loadConfig();
      }
    } catch (ConfigUtil.InvalidConfig e) {
      e.logErrors();
      // disable plugins to prevent firing onServerLoad and similar events
      Bukkit.getPluginManager().disablePlugins();
      Bukkit.shutdown();
    }
  }

  @EventHandler
  public void onServerLoad(@NotNull ServerLoadEvent event) {
    Players.updateWhitelisted();
    Logger.info("clod-mc started");
  }

  @EventHandler
  public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
    Players.updateWhitelisted();
  }

  public static @Nullable <T extends Module> T getModule(@NotNull Class<T> moduleClass) {
    return instance.moduleRegistry.get(moduleClass);
  }
}
