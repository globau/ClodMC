package au.com.glob.clodmc;

import au.com.glob.clodmc.config.Config;
import au.com.glob.clodmc.modules.BlueMapModule;
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
import au.com.glob.clodmc.modules.player.PlayerTracker;
import au.com.glob.clodmc.modules.player.SeenCommand;
import au.com.glob.clodmc.modules.player.Sleep;
import au.com.glob.clodmc.modules.server.CircularWorldBorder;
import au.com.glob.clodmc.modules.server.ConfigureServer;
import au.com.glob.clodmc.modules.server.RequiredPlugins;
import au.com.glob.clodmc.modules.server.SpawnMarker;
import au.com.glob.clodmc.modules.welcome.WelcomeCommand;
import au.com.glob.clodmc.modules.welcome.WelcomeGift;
import au.com.glob.clodmc.util.BlueMap;
import au.com.glob.clodmc.util.MaterialUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class ClodMC extends JavaPlugin implements Listener {
  @SuppressWarnings("NotNullFieldNotInitialized")
  public static @NotNull ClodMC instance;

  private final @NotNull List<Module> modules = new ArrayList<>();
  private boolean shuttingDown;

  // init

  public ClodMC() {
    super();
    instance = this;
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
    MaterialUtil.init();
    Bukkit.getPluginManager().registerEvents(this, this);

    // core - used by other modules
    this.register(new OpAlerts());

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
    this.register(new PlayerTracker());
    this.register(new SeenCommand());
    this.register(new Sleep());

    // server
    this.register(new CircularWorldBorder());
    this.register(new ConfigureServer());
    this.register(new RequiredPlugins());
    this.register(new SpawnMarker());

    // welcome
    this.register(new WelcomeGift());
    this.register(new WelcomeCommand());

    // load configs after sanity checking
    try {
      Config.sanityCheckConfigs();
    } catch (RuntimeException e) {
      // turns out this doesn't set Bukkit.isStopping, and bukkit will complete
      // the startup process before immediately shutting down; this might cause
      // the wrong error to be shown in addition to the sanity check failures.
      Bukkit.shutdown();
      this.shuttingDown = true;
      return;
    }
    Config.preload("config.yml");
    for (Module module : this.modules) {
      module.loadConfig();
    }

    // bluemap
    if (Bukkit.getPluginManager().isPluginEnabled("BlueMap")) {
      BlueMap.onEnable(
          this.modules.stream()
              .filter((Module module) -> module instanceof BlueMapModule)
              .map((Module module) -> (BlueMapModule) module)
              .toList());
    } else {
      ClodMC.logWarning("Cannot load BlueMap integration: BlueMap is not enabled");
    }
  }

  @EventHandler
  public void onServerLoad(@NotNull ServerLoadEvent event) {
    // sadly can't use Bukkit.isStopping()
    if (!this.shuttingDown) {
      ClodMC.logInfo("clod-mc started");
    }
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

    this.modules.add(module);

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

  // helpers

  @Override
  public @NotNull Config getConfig() {
    return Config.of("config.yml");
  }

  // messages

  private enum Style {
    FYI("<grey>"),
    WHISPER("<grey><i>"),
    INFO("<yellow>"),
    WARNING("<yellow><i>"),
    ERROR("<red>");

    final @NotNull String prefix;

    Style(@NotNull String prefix) {
      this.prefix = prefix;
    }
  }

  private static void sendMessage(
      @NotNull CommandSender sender, @NotNull Style style, @NotNull String message) {
    sender.sendRichMessage(style.prefix + message);
  }

  public static void fyi(@NotNull CommandSender sender, @NotNull String message) {
    sendMessage(sender, Style.FYI, message);
  }

  public static void whisper(@NotNull CommandSender sender, @NotNull String message) {
    sendMessage(sender, Style.WHISPER, message);
  }

  public static void info(@NotNull CommandSender sender, @NotNull String message) {
    sendMessage(sender, Style.INFO, message);
  }

  public static void warning(@NotNull CommandSender sender, @NotNull String message) {
    sendMessage(sender, Style.WARNING, message);
  }

  public static void error(@NotNull CommandSender sender, @NotNull String message) {
    sendMessage(sender, Style.ERROR, message);
  }

  // logging

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
