package au.com.glob.clodmc.util;

import au.com.glob.clodmc.ClodMC;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerData implements Listener {
  private static PlayerData instance;
  private final File playerConfigPath;
  private final Map<UUID, FileConfiguration> playerConfigs = new HashMap<>();

  public static void init() {
    Bukkit.getServer().getPluginManager().registerEvents(new PlayerData(), ClodMC.instance);
  }

  public static @NotNull MemoryConfiguration get(@NotNull Player player) {
    return instance.playerConfigs.get(player.getUniqueId());
  }

  public static void save(@NotNull Player player) {
    if (instance.playerConfigs.containsKey(player.getUniqueId())) {
      try {
        instance.playerConfigs.get(player.getUniqueId()).save(instance.getPlayerConfigFile(player));
      } catch (IOException e) {
        ClodMC.logError(instance.playerConfigPath + ": save failed: " + e);
      }
    }
  }

  public PlayerData() {
    instance = this;

    this.playerConfigPath = new File(ClodMC.instance.getDataFolder(), "players");
    if (!this.playerConfigPath.exists() && !this.playerConfigPath.mkdirs()) {
      ClodMC.logWarning(this.playerConfigPath + ": mkdir failed");
    }
  }

  private @NotNull File getPlayerConfigFile(@NotNull Player player) {
    return new File(this.playerConfigPath, player.getUniqueId() + ".yml");
  }

  private void migrate(YamlConfiguration config, Player player) {
    boolean saveChanges = false;

    if (config.contains("internal.back")) {
      config.set("homes_internal.back", config.get("internal.back"));
      config.set("internal.back", null);
      saveChanges = true;
    }

    if (config.contains("internal")) {
      config.set("internal", null);
      saveChanges = true;
    }

    if (saveChanges) {
      PlayerData.save(player);
    }
  }

  @EventHandler
  public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
    Player player = event.getPlayer();
    YamlConfiguration config =
        YamlConfiguration.loadConfiguration(this.getPlayerConfigFile(player));
    this.playerConfigs.put(player.getUniqueId(), config);
    this.migrate(config, player);

    config.set("player.name", player.getName());
    config.set(
        "player.last_login",
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    save(player);
  }

  @EventHandler
  public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
    this.playerConfigs.remove(event.getPlayer().getUniqueId());
  }

  @EventHandler
  public void onPluginDisable(PluginDisableEvent pluginDisableEvent) {
    for (Player player : Bukkit.getOnlinePlayers()) {
      save(player);
    }
  }
}
