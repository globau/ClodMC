package au.com.glob.clodmc;

import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.ModuleRegistry;
import au.com.glob.clodmc.modules.SimpleCommand;
import au.com.glob.clodmc.util.ConfigUtil;
import au.com.glob.clodmc.util.MaterialUtil;
import java.io.File;
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

  private final @NotNull ModuleRegistry moduleRegistry = new ModuleRegistry();

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
    this.moduleRegistry.registerAll();

    // ensure all configs can be deserialised, halt server if not to avoid dataloss
    try {
      ConfigUtil.sanityCheckConfigs();

      for (Module module : this.moduleRegistry.all()) {
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
    ClodMC.logInfo("clod-mc started");
  }

  public static @NotNull <T extends Module> T getModule(@NotNull Class<T> moduleClass) {
    return instance.moduleRegistry.get(moduleClass);
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
