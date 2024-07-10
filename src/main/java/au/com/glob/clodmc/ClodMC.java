package au.com.glob.clodmc;

import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.SimpleCommand;
import au.com.glob.clodmc.modules.gateways.Gateways;
import au.com.glob.clodmc.modules.homes.BackCommand;
import au.com.glob.clodmc.modules.homes.DelHomeCommand;
import au.com.glob.clodmc.modules.homes.HomeCommand;
import au.com.glob.clodmc.modules.homes.Homes;
import au.com.glob.clodmc.modules.homes.HomesCommand;
import au.com.glob.clodmc.modules.homes.SetHomeCommand;
import au.com.glob.clodmc.modules.homes.SpawnCommand;
import au.com.glob.clodmc.modules.interactions.Decorations;
import au.com.glob.clodmc.modules.interactions.FastLeafDecay;
import au.com.glob.clodmc.modules.inventory.AdminInvCommand;
import au.com.glob.clodmc.modules.inventory.InventorySort;
import au.com.glob.clodmc.modules.mobs.BetterDrops;
import au.com.glob.clodmc.modules.mobs.PreventMobGriefing;
import au.com.glob.clodmc.modules.mobs.PreventMobSpawn;
import au.com.glob.clodmc.modules.player.AFK;
import au.com.glob.clodmc.modules.player.InviteCommand;
import au.com.glob.clodmc.modules.player.OfflineMessages;
import au.com.glob.clodmc.modules.player.OpAlerts;
import au.com.glob.clodmc.modules.player.PlayerData;
import au.com.glob.clodmc.modules.player.SeenCommand;
import au.com.glob.clodmc.modules.player.Sleep;
import au.com.glob.clodmc.modules.server.CircularWorldBorder;
import au.com.glob.clodmc.modules.server.ConfigureServer;
import au.com.glob.clodmc.modules.server.RequiredPlugins;
import au.com.glob.clodmc.modules.welcome.WelcomeCommand;
import au.com.glob.clodmc.modules.welcome.WelcomeGift;
import au.com.glob.clodmc.util.Config;
import au.com.glob.clodmc.util.MaterialUtil;
import au.com.glob.clodmc.util.PlayerLocation;
import java.io.File;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class ClodMC extends JavaPlugin implements Listener {
  public static ClodMC instance;

  public ClodMC() {
    super();
    instance = this;

    ConfigurationSerialization.registerClass(PlayerLocation.class);
  }

  @Override
  public void onLoad() {
    File dataFolder = this.getDataFolder();
    if (!dataFolder.exists() && !dataFolder.mkdirs()) {
      logWarning("failed to create " + dataFolder);
    }
  }

  @Override
  public void onEnable() {
    Config.init("config.yml");
    MaterialUtil.init();
    Bukkit.getPluginManager().registerEvents(this, this);

    // core - used by other modules
    this.register(new OpAlerts());
    this.register(new PlayerData());

    // gateways
    this.register(new Gateways());

    // homes
    this.register(new Homes());
    this.register(new BackCommand());
    this.register(new DelHomeCommand());
    this.register(new HomeCommand());
    this.register(new SpawnCommand());
    this.register(new HomesCommand());
    this.register(new SetHomeCommand());

    // interactions
    this.register(new Decorations());
    this.register(new FastLeafDecay());

    // inventory
    this.register(new AdminInvCommand());
    this.register(new InventorySort());

    // mobs
    this.register(new BetterDrops());
    this.register(new PreventMobGriefing());
    this.register(new PreventMobSpawn());

    // player
    this.register(new AFK());
    this.register(new InviteCommand());
    this.register(new OfflineMessages());
    this.register(new SeenCommand());
    this.register(new Sleep());

    // server
    this.register(new CircularWorldBorder());
    this.register(new ConfigureServer());
    this.register(new RequiredPlugins());

    // welcome
    this.register(new WelcomeGift());
    this.register(new WelcomeCommand());
  }

  @EventHandler
  public void onServerLoad(ServerLoadEvent event) {
    ClodMC.logInfo("clod-mc started");
  }

  private void register(@NotNull Module module) {
    String dependsOn = module.dependsOn();
    if (dependsOn != null && !Bukkit.getPluginManager().isPluginEnabled(dependsOn)) {
      logWarning(
          "Cannot load "
              + module.getClass().getSimpleName()
              + ": depends on plugin "
              + dependsOn
              + " which is not enabled");
      return;
    }

    if (module instanceof Listener listener) {
      Bukkit.getServer().getPluginManager().registerEvents(listener, this);
    }

    if (module instanceof SimpleCommand command) {
      this.getServer().getCommandMap().register("clod-mc", command);
    }
  }

  @Override
  public void onDisable() {
    SimpleCommand.unregisterAll();
  }

  @Override
  public @NotNull Config getConfig() {
    return Config.getInstance("config.yml");
  }

  public static void logInfo(@NotNull String message) {
    instance.getLogger().info(message);
  }

  public static void logWarning(@NotNull String message) {
    instance.getLogger().warning(message);
  }

  public static void logError(@NotNull String message) {
    instance.getLogger().severe(message);
  }

  public static void logException(@NotNull Throwable exception) {
    instance
        .getLogger()
        .log(
            Level.SEVERE,
            exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage(),
            exception);
  }
}
