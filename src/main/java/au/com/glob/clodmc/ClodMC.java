package au.com.glob.clodmc;

import au.com.glob.clodmc.config.PluginConfig;
import au.com.glob.clodmc.modules.homes.BackCommand;
import au.com.glob.clodmc.modules.homes.DelHomeCommand;
import au.com.glob.clodmc.modules.homes.HomeCommand;
import au.com.glob.clodmc.modules.homes.Homes;
import au.com.glob.clodmc.modules.homes.HomesCommand;
import au.com.glob.clodmc.modules.homes.SetHomeCommand;
import au.com.glob.clodmc.modules.homes.SpawnCommand;
import au.com.glob.clodmc.modules.invite.InviteCommand;
import au.com.glob.clodmc.modules.spawn.PreventMobSpawn;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import java.io.File;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class ClodMC extends JavaPlugin {
  private static ClodMC instance;
  private File dataFolder;

  public ClodMC() {
    super();
    instance = this;
  }

  @Override
  public void onLoad() {
    this.dataFolder = this.getDataFolder();
    if (!this.dataFolder.exists()) {
      if (!this.dataFolder.mkdirs()) {
        logWarning("failed to create " + this.dataFolder);
      }
    }

    CommandAPI.onLoad(new CommandAPIBukkitConfig(this).verboseOutput(true));
  }

  @Override
  public void onEnable() {
    PluginConfig.getInstance().loadConfig(this.dataFolder);
    PluginConfig.getInstance().reload();

    CommandAPI.onEnable();

    Homes.register();
    BackCommand.register();
    DelHomeCommand.register();
    HomeCommand.register();
    SpawnCommand.register();
    HomesCommand.register();
    SetHomeCommand.register();

    InviteCommand.register();

    PreventMobSpawn.register();
  }

  @Override
  public void onDisable() {
    CommandAPI.onDisable();
  }

  public static @NotNull ClodMC getInstance() {
    return instance;
  }

  public static void logWarning(@NotNull String message) {
    instance.getLogger().warning(message);
  }
}
