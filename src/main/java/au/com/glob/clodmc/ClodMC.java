package au.com.glob.clodmc;

import au.com.glob.clodmc.modules.admin.OutlineClaimCommand;
import au.com.glob.clodmc.modules.homes.BackCommand;
import au.com.glob.clodmc.modules.homes.DelHomeCommand;
import au.com.glob.clodmc.modules.homes.HomeCommand;
import au.com.glob.clodmc.modules.homes.Homes;
import au.com.glob.clodmc.modules.homes.HomesCommand;
import au.com.glob.clodmc.modules.homes.SetHomeCommand;
import au.com.glob.clodmc.modules.homes.SpawnCommand;
import au.com.glob.clodmc.modules.invite.InviteCommand;
import au.com.glob.clodmc.modules.mobs.BetterDrops;
import au.com.glob.clodmc.modules.mobs.PreventMobGriefing;
import au.com.glob.clodmc.modules.mobs.PreventMobSpawn;
import au.com.glob.clodmc.modules.server.ConfigureServer;
import au.com.glob.clodmc.modules.server.RequiredPlugins;
import au.com.glob.clodmc.modules.welcome.WelcomeCommand;
import au.com.glob.clodmc.modules.welcome.WelcomeGift;
import au.com.glob.clodmc.util.Config;
import au.com.glob.clodmc.util.PlayerLocation;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import java.io.File;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class ClodMC extends JavaPlugin {
  static {
    ConfigurationSerialization.registerClass(PlayerLocation.class, "Location");
  }

  public static ClodMC instance;

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

    CommandAPI.onLoad(new CommandAPIBukkitConfig(this));
  }

  @Override
  public void onEnable() {
    Config.init("config.yml");

    CommandAPI.onEnable();

    RequiredPlugins.register();
    ConfigureServer.register();

    Homes.register();
    BackCommand.register();
    DelHomeCommand.register();
    HomeCommand.register();
    SpawnCommand.register();
    HomesCommand.register();
    SetHomeCommand.register();

    InviteCommand.register();

    BetterDrops.register();
    PreventMobGriefing.register();
    PreventMobSpawn.register();

    WelcomeGift.register();
    WelcomeCommand.register();

    OutlineClaimCommand.register();
  }

  @Override
  public void onDisable() {
    CommandAPI.onDisable();
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
}
