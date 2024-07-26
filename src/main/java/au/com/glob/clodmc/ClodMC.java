package au.com.glob.clodmc;

import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.SimpleCommand;
import au.com.glob.clodmc.modules.bluemap.BlueMap;
import au.com.glob.clodmc.modules.gateways.Gateways;
import au.com.glob.clodmc.modules.homes.DelHomeCommand;
import au.com.glob.clodmc.modules.homes.HomeCommand;
import au.com.glob.clodmc.modules.homes.Homes;
import au.com.glob.clodmc.modules.homes.HomesCommand;
import au.com.glob.clodmc.modules.homes.SetHomeCommand;
import au.com.glob.clodmc.modules.interactions.Decorations;
import au.com.glob.clodmc.modules.interactions.FastLeafDecay;
import au.com.glob.clodmc.modules.interactions.NamedStorage;
import au.com.glob.clodmc.modules.inventory.AdminInvCommand;
import au.com.glob.clodmc.modules.inventory.InventorySort;
import au.com.glob.clodmc.modules.mobs.BetterDrops;
import au.com.glob.clodmc.modules.mobs.PreventMobGriefing;
import au.com.glob.clodmc.modules.mobs.PreventMobSpawn;
import au.com.glob.clodmc.modules.player.AFK;
import au.com.glob.clodmc.modules.player.BackCommand;
import au.com.glob.clodmc.modules.player.GameModeCommand;
import au.com.glob.clodmc.modules.player.InviteCommand;
import au.com.glob.clodmc.modules.player.OfflineMessages;
import au.com.glob.clodmc.modules.player.OpAlerts;
import au.com.glob.clodmc.modules.player.PlayerTracker;
import au.com.glob.clodmc.modules.player.SeenCommand;
import au.com.glob.clodmc.modules.player.Sleep;
import au.com.glob.clodmc.modules.player.SpawnCommand;
import au.com.glob.clodmc.modules.player.WelcomeBook;
import au.com.glob.clodmc.modules.server.CircularWorldBorder;
import au.com.glob.clodmc.modules.server.RequiredPlugins;
import au.com.glob.clodmc.util.MaterialUtil;
import au.com.glob.clodmc.util.PlayerLocation;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.error.YAMLException;

public final class ClodMC extends JavaPlugin implements Listener {
  @SuppressWarnings("NotNullFieldNotInitialized")
  public static @NotNull ClodMC instance;

  public static boolean sanityChecked;
  private final @NotNull Map<Class<? extends Module>, Module> modules = new HashMap<>();
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
    this.register(new DelHomeCommand());
    this.register(new HomeCommand());
    this.register(new HomesCommand());
    this.register(new SetHomeCommand());

    // interactions
    this.register(new Decorations());
    this.register(new FastLeafDecay());
    this.register(new NamedStorage());

    // inventory
    this.register(new AdminInvCommand());
    this.register(new InventorySort());

    // mobs
    this.register(new BetterDrops());
    this.register(new PreventMobGriefing());
    this.register(new PreventMobSpawn());

    // player
    this.register(new AFK());
    this.register(new BackCommand());
    this.register(new GameModeCommand());
    this.register(new InviteCommand());
    this.register(new OfflineMessages());
    this.register(new PlayerTracker());
    this.register(new SeenCommand());
    this.register(new Sleep());
    this.register(new SpawnCommand());
    this.register(new WelcomeBook());

    // server
    this.register(new CircularWorldBorder());
    this.register(new RequiredPlugins());

    // bluemap
    this.register(new BlueMap());

    // load configs after sanity checking
    try {
      this.sanityCheckConfigs();
    } catch (RuntimeException e) {
      // turns out this doesn't set Bukkit.isStopping, and bukkit will complete
      // the startup process before immediately shutting down; this might cause
      // the wrong error to be shown in addition to the sanity check failures.
      Bukkit.shutdown();
      this.shuttingDown = true;
      return;
    }
    for (Module module : this.modules.values()) {
      module.loadConfig();
    }
  }

  private void sanityCheckConfigs() throws RuntimeException {
    ConfigurationSerialization.registerClass(PlayerLocation.class);

    // ensure all configs can be deserialised, halt server if not to avoid dataloss
    List<String> errors = new ArrayList<>(0);
    try (Stream<Path> paths = Files.walk(ClodMC.instance.getDataFolder().toPath())) {
      paths
          .filter(Files::isRegularFile)
          .filter((Path path) -> path.toString().endsWith(".yml"))
          .sorted()
          .map(Path::toFile)
          .forEach(
              (File file) -> {
                try {
                  YamlConfiguration.loadConfiguration(file);
                } catch (YAMLException e) {
                  StringJoiner message = new StringJoiner(": ");
                  message.add(file.toString());
                  message.add(e.getMessage());
                  if (e.getCause() != null) {
                    message.add(e.getCause().getMessage());
                  }
                  errors.add(message.toString());
                }
              });
    } catch (IOException e) {
      errors.add(e.getMessage());
    }
    if (!errors.isEmpty()) {
      for (String error : errors) {
        ClodMC.logError(error);
      }
      throw new RuntimeException();
    }
    sanityChecked = true;
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

    this.modules.put(module.getClass(), module);

    if (module instanceof Listener listener) {
      Bukkit.getServer().getPluginManager().registerEvents(listener, this);
    }

    if (module instanceof SimpleCommand command) {
      this.getServer().getCommandMap().register("clod-mc", command);
    }
  }

  @SuppressWarnings("unchecked")
  public static @NotNull <T extends Module> T getModule(@NotNull Class<T> moduleClass) {
    Module module = instance.modules.get(moduleClass);
    assert module != null;
    return (T) module;
  }

  @Override
  public void onDisable() {
    SimpleCommand.unregisterAll();
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
