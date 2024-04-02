package au.com.glob.clodmc;

import au.com.glob.clodmc.config.PluginConfig;
import au.com.glob.clodmc.modules.homes.BackCommand;
import au.com.glob.clodmc.modules.homes.DelHomeCommand;
import au.com.glob.clodmc.modules.homes.HomeCommand;
import au.com.glob.clodmc.modules.homes.HomesCommand;
import au.com.glob.clodmc.modules.homes.SetHomeCommand;
import au.com.glob.clodmc.modules.homes.SpawnCommand;
import au.com.glob.clodmc.modules.invite.InviteCommand;
import au.com.glob.clodmc.modules.spawn.PreventMobSpawn;
import java.io.File;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class ClodMC extends JavaPlugin {
  private static ClodMC instance;

  @Override
  public void onEnable() {
    instance = this;
    File dataFolder = this.getDataFolder();
    if (!dataFolder.exists()) {
      if (!dataFolder.mkdirs()) {
        logWarning("failed to create " + dataFolder);
      }
    }
    PluginConfig.getInstance().loadConfig(dataFolder);

    this.addCommand("back", new BackCommand());
    this.addCommand("home", new HomeCommand());
    this.addCommand("delhome", new DelHomeCommand());
    this.addCommand("homes", new HomesCommand());
    this.addCommand("sethome", new SetHomeCommand());
    this.addCommand("spawn", new SpawnCommand());

    this.addCommand("invite", new InviteCommand());

    new PreventMobSpawn();

    PluginConfig.getInstance().reload();
  }

  private void addCommand(@NotNull String name, @NotNull CommandExecutor executor) {
    PluginCommand command = this.getCommand(name);
    if (command == null) {
      throw new RuntimeException("'" + name + "' not defined in plugin.yml");
    }
    command.setExecutor(executor);
  }

  public static @NotNull ClodMC getInstance() {
    return instance;
  }

  public static void logWarning(@NotNull String message) {
    instance.getLogger().warning(message);
  }
}
