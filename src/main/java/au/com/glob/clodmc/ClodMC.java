package au.com.glob.clodmc;

import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.ModuleRegistry;
import au.com.glob.clodmc.util.Bedrock;
import au.com.glob.clodmc.util.ConfigUtil;
import au.com.glob.clodmc.util.InvalidConfigException;
import au.com.glob.clodmc.util.Logger;
import au.com.glob.clodmc.util.Players;
import java.io.File;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import vendored.com.jeff_media.customblockdata.CustomBlockData;

/** main plugin class that handles lifecycle and module coordination */
@NullMarked
public final class ClodMC extends JavaPlugin implements Listener {
  @SuppressWarnings({"NotNullFieldNotInitialized", "NullAway.Init"})
  public static ClodMC instance;

  private final ModuleRegistry moduleRegistry = new ModuleRegistry();
  private boolean geyserLoaded;

  // initialise plugin instance
  public ClodMC() {
    super();
    instance = this;
  }

  // create data folder if needed
  @Override
  public void onLoad() {
    File dataFolder = this.getDataFolder();
    if (!dataFolder.exists() && !dataFolder.mkdirs()) {
      Logger.warning("failed to create %s".formatted(dataFolder));
    }
  }

  // register modules and load configs with validation
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
    } catch (InvalidConfigException e) {
      e.logErrors();
      // disable plugins to prevent firing onServerLoad and similar events
      Bukkit.getPluginManager().disablePlugins();
      Bukkit.shutdown();
    }
  }

  // test geyser availability and update player list
  @EventHandler
  public void onServerLoad(ServerLoadEvent event) {
    try {
      Bedrock.apiTest();
      this.geyserLoaded = true;
    } catch (NoClassDefFoundError e) {
      this.geyserLoaded = false;
    }
    Players.updateWhitelisted();
    Logger.info("clod-mc started");
  }

  // update whitelist when player joins
  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Players.updateWhitelisted();
  }

  // check if geyser plugin is available
  public boolean isGeyserLoaded() {
    return this.geyserLoaded;
  }
}
