package au.com.glob.clodmc.config;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.IniFile;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PluginConfig {
  private static PluginConfig INSTANCE;

  private IniFile iniFile;

  @Nullable private File playerConfigPath;
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
    this.iniFile = new IniFile(new File(dataFolder, "config.ini"), null);

    if (!this.iniFile.exists()) {
      this.iniFile.setInteger("homes", "max-allowed", this.getMaxAllowedHomes());
      this.iniFile.setString("homes", "overworld-name", this.getOverworldName());
    }

    this.playerConfigPath = new File(dataFolder, "players");
    if (!this.playerConfigPath.exists() && !this.playerConfigPath.mkdirs()) {
      ClodMC.getInstance().logWarning(this.playerConfigPath + ": mkdir failed");
    }
  }

  public int getMaxAllowedHomes() {
    return this.iniFile.getInteger("homes", "max-allowed", 2);
  }

  public @NotNull String getOverworldName() {
    return this.iniFile.getString("homes", "overworld-name", "world");
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
