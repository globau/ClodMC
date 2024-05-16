package au.com.glob.clodmc.modules.homes;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.PlayerLocation;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Homes implements Listener, Module {
  protected static Homes instance;

  @Nullable private final File playerConfigPath;
  private final Map<String, FileConfiguration> playerConfigs = new HashMap<>();

  public Homes() {
    super();

    instance = this;

    this.playerConfigPath = new File(ClodMC.instance.getDataFolder(), "players");
    if (!this.playerConfigPath.exists() && !this.playerConfigPath.mkdirs()) {
      ClodMC.logWarning(this.playerConfigPath + ": mkdir failed");
    }

    for (Player player : Bukkit.getServer().getOnlinePlayers()) {
      Homes.instance.onPlayerJoin(player);
    }
  }

  private @NotNull File getPlayerConfigFile(@NotNull Player player) {
    return new File(this.playerConfigPath, player.getUniqueId() + ".yml");
  }

  private @NotNull FileConfiguration getConfig(@NotNull Player player) {
    return this.playerConfigs.get(player.getName());
  }

  protected @NotNull Map<String, Location> getHomes(@NotNull Player player) {
    FileConfiguration config = this.getConfig(player);
    ConfigurationSection section = config.getConfigurationSection("homes");
    if (section == null) {
      return new HashMap<>(0);
    }
    return section.getKeys(false).stream()
        .collect(
            Collectors.toMap(
                (String name) -> name,
                (String name) -> {
                  Location location = config.getLocation("homes." + name, null);
                  assert location != null;
                  return location;
                }));
  }

  protected void setHomes(@NotNull Player player, @NotNull Map<String, Location> homes) {
    FileConfiguration config = this.getConfig(player);
    ConfigurationSection section = config.getConfigurationSection("homes");
    if (section != null) {
      for (String name : section.getKeys(false)) {
        if (!homes.containsKey(name)) {
          section.set(name, null);
        }
      }
    }
    for (String name : homes.keySet()) {
      config.set("homes." + name, homes.get(name));
    }
    this.saveConfig(player, config);
  }

  protected void setBackLocation(@NotNull Player player) {
    PlayerLocation playerLocation = PlayerLocation.of(player);
    FileConfiguration config = this.getConfig(player);
    config.set("internal.back", playerLocation);
    this.saveConfig(player, config);
  }

  protected @Nullable PlayerLocation getBackLocation(@NotNull Player player) {
    FileConfiguration config = this.getConfig(player);
    return (PlayerLocation) config.get("internal.back");
  }

  private void saveConfig(@NotNull Player player, @NotNull FileConfiguration config) {
    try {
      config.save(this.getPlayerConfigFile(player));
    } catch (IOException e) {
      ClodMC.logWarning(this.playerConfigPath + ": save failed: " + e);
    }
  }

  @EventHandler
  public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
    this.onPlayerJoin(event.getPlayer());
  }

  private void onPlayerJoin(@NotNull Player player) {
    YamlConfiguration config =
        YamlConfiguration.loadConfiguration(this.getPlayerConfigFile(player));
    config.set("internal.player", player.getName());
    this.playerConfigs.put(player.getName(), config);
  }

  @EventHandler
  public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
    this.playerConfigs.remove(event.getPlayer().getName());
  }
}
