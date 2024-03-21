package au.com.glob.clodmc.config;

import au.com.glob.clodmc.ClodMC;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PluginConfig {
  private static PluginConfig INSTANCE;

  @Nullable private File playerConfigPath;
  private int maxHomes;
  private String overworldName;

  private final Map<String, PlayerConfig> playerConfigs = new HashMap<>();

  public @NotNull static PluginConfig getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new PluginConfig();
    }

    return INSTANCE;
  }

  public void reload() {
    this.playerConfigs.clear();
    for (Player player : Bukkit.getServer().getOnlinePlayers()) {
      this.onPlayerJoin(player);
    }
  }

  public void loadConfig(@NotNull File dataFolder) {
    ConfigFile configFile =
        new ConfigFile(new File(dataFolder, "config.properties"), "au.com.glob.homes config");

    this.maxHomes = configFile.get("max-homes", 2);
    this.overworldName = configFile.get("overworld-name", "world");

    if (!configFile.exists()) {
      configFile.set("max-homes", this.maxHomes);
      configFile.set("overworld-name", "world");
      configFile.save();
    }

    this.playerConfigPath = new File(dataFolder, "players");
    if (!this.playerConfigPath.exists() && !this.playerConfigPath.mkdirs()) {
      ClodMC.getInstance().logWarning(this.playerConfigPath + ": mkdir failed");
    }
  }

  public int getMaxHomes() {
    return this.maxHomes;
  }

  public @NotNull String getOverworldName() {
    return this.overworldName;
  }

  public @Nullable PlayerConfig getPlayerConfig(@NotNull Player player) {
    return this.playerConfigs.get(player.getName());
  }

  public void onPlayerJoin(@NotNull Player player) {
    assert this.playerConfigPath != null;
    this.playerConfigs.put(player.getName(), new PlayerConfig(this.playerConfigPath, player));
  }

  public void onPlayerPart(@NotNull Player player) {
    this.playerConfigs.remove(player.getName());
  }
}
