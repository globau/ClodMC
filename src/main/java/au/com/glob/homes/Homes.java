package au.com.glob.homes;

import au.com.glob.homes.commands.BackCommand;
import au.com.glob.homes.commands.DelHomeCommand;
import au.com.glob.homes.commands.HomeCommand;
import au.com.glob.homes.commands.HomesCommand;
import au.com.glob.homes.commands.SetHomeCommand;
import au.com.glob.homes.commands.SpawnCommand;
import au.com.glob.homes.config.PluginConfig;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class Homes extends JavaPlugin implements Listener {
  private static Homes INSTANCE;

  @Override
  public void onEnable() {
    INSTANCE = this;
    getServer().getPluginManager().registerEvents(this, this);
    PluginConfig.getInstance().loadConfig(this.getDataFolder());

    PluginCommand backCommand = this.getCommand("back");
    assert backCommand != null;
    backCommand.setExecutor(new BackCommand());
    backCommand.setTabCompleter(new NullTabCompleter());

    PluginCommand homeCommand = this.getCommand("home");
    assert homeCommand != null;
    homeCommand.setExecutor(new HomeCommand());
    homeCommand.setTabCompleter(new HomeTabCompleter());

    PluginCommand delHomeCommand = this.getCommand("delhome");
    assert delHomeCommand != null;
    delHomeCommand.setExecutor(new DelHomeCommand());
    delHomeCommand.setTabCompleter(new HomeTabCompleter());

    PluginCommand homesCommand = this.getCommand("homes");
    assert homesCommand != null;
    homesCommand.setExecutor(new HomesCommand());
    homesCommand.setTabCompleter(new NullTabCompleter());

    PluginCommand setHomeCommand = this.getCommand("sethome");
    assert setHomeCommand != null;
    setHomeCommand.setExecutor(new SetHomeCommand());
    setHomeCommand.setTabCompleter(new NullTabCompleter());

    PluginCommand spawnCommand = this.getCommand("spawn");
    assert spawnCommand != null;
    spawnCommand.setExecutor(new SpawnCommand());
    spawnCommand.setTabCompleter(new NullTabCompleter());

    PluginConfig.getInstance().reload();
  }

  public static @NotNull Homes getInstance() {
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
