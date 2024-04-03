package au.com.glob.clodmc.modules.homes;

import au.com.glob.clodmc.ClodMC;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
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

public class Homes implements Listener {
  public static Homes instance;

  @Nullable private final File playerConfigPath;
  private final Map<String, FileConfiguration> playerConfigs = new HashMap<>();

  public static void register() {
    FileConfiguration config = ClodMC.instance.getConfig();
    if (!config.contains("homes.max-allowed")) {
      config.set("homes.max-allowed", config.get("homes.max-allowed", 2));
      config.set("homes.overworld-name", config.get("homes.overworld-name", "world"));
      ClodMC.instance.saveConfig();
    }

    Bukkit.getServer().getPluginManager().registerEvents(new Homes(), ClodMC.instance);

    for (Player player : Bukkit.getServer().getOnlinePlayers()) {
      Homes.instance.onPlayerJoin(player);
    }
  }

  private Homes() {
    instance = this;

    this.playerConfigPath = new File(ClodMC.instance.getDataFolder(), "players");
    if (!this.playerConfigPath.exists() && !this.playerConfigPath.mkdirs()) {
      ClodMC.logWarning(this.playerConfigPath + ": mkdir failed");
    }
  }

  private @NotNull File getPlayerConfigFile(@NotNull Player player) {
    return new File(this.playerConfigPath, player.getUniqueId() + ".yml");
  }

  public @NotNull FileConfiguration getConfig(@NotNull Player player) {
    return this.playerConfigs.get(player.getName());
  }

  public void saveConfig(@NotNull Player player, @NotNull FileConfiguration config) {
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

  public static @NotNull Argument<String> homesArgument(@NotNull String name) {
    return new StringArgument(name)
        .replaceSuggestions(
            ArgumentSuggestions.strings(
                (info) -> {
                  if (info.sender() instanceof Player player) {
                    FileConfiguration config = instance.getConfig(player);
                    ConfigurationSection section = config.getConfigurationSection("homes");
                    return section == null
                        ? new String[0]
                        : section.getKeys(false).toArray(new String[0]);
                  } else {
                    return new String[0];
                  }
                }));
  }
}
