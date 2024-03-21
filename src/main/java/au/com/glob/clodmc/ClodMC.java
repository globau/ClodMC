package au.com.glob.clodmc;

import au.com.glob.clodmc.commands.BackCommand;
import au.com.glob.clodmc.commands.DelHomeCommand;
import au.com.glob.clodmc.commands.HomeCommand;
import au.com.glob.clodmc.commands.HomesCommand;
import au.com.glob.clodmc.commands.SetHomeCommand;
import au.com.glob.clodmc.commands.SpawnCommand;
import au.com.glob.clodmc.config.PluginConfig;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class ClodMC extends JavaPlugin implements Listener {
  private static ClodMC INSTANCE;

  @Override
  public void onEnable() {
    INSTANCE = this;
    getServer().getPluginManager().registerEvents(this, this);
    PluginConfig.getInstance().loadConfig(this.getDataFolder());

    PluginCommand backCommand = this.getCommand("back");
    assert backCommand != null;
    backCommand.setExecutor(new BackCommand());

    PluginCommand homeCommand = this.getCommand("home");
    assert homeCommand != null;
    homeCommand.setExecutor(new HomeCommand());

    PluginCommand delHomeCommand = this.getCommand("delhome");
    assert delHomeCommand != null;
    delHomeCommand.setExecutor(new DelHomeCommand());

    PluginCommand homesCommand = this.getCommand("homes");
    assert homesCommand != null;
    homesCommand.setExecutor(new HomesCommand());

    PluginCommand setHomeCommand = this.getCommand("sethome");
    assert setHomeCommand != null;
    setHomeCommand.setExecutor(new SetHomeCommand());

    PluginCommand spawnCommand = this.getCommand("spawn");
    assert spawnCommand != null;
    spawnCommand.setExecutor(new SpawnCommand());

    PluginConfig.getInstance().reload();
  }

  public static @NotNull ClodMC getInstance() {
    return INSTANCE;
  }

  @EventHandler
  public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
    PluginConfig.getInstance().onPlayerJoin(event.getPlayer());
  }

  @EventHandler
  public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
    PluginConfig.getInstance().onPlayerPart(event.getPlayer());
  }

  public void logWarning(@NotNull String message) {
    this.getLogger().warning(message);
  }
}
