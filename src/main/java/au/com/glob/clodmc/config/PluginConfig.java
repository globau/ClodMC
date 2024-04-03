package au.com.glob.clodmc.config;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.util.IniFile;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PluginConfig implements Listener {

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

  private PluginConfig() {
    Bukkit.getServer().getPluginManager().registerEvents(this, ClodMC.getInstance());
  }

  public void reload() {
    this.playerConfigs.clear();
    for (Player player : Bukkit.getServer().getOnlinePlayers()) {
      this.onPlayerJoin(player);
    }
  }

  public void loadConfig(@NotNull File dataFolder) {
    this.iniFile = new IniFile(new File(dataFolder, "config.ini"), null);

    this.playerConfigPath = new File(dataFolder, "players");
    if (!this.playerConfigPath.exists() && !this.playerConfigPath.mkdirs()) {
      ClodMC.logWarning(this.playerConfigPath + ": mkdir failed");
    }

    this.setDefaultValue("mailer", "hostname", "in1-smtp.messagingengine.com");
    this.setDefaultValue("mailer", "sender-name", "Clod Minecraft Server");
    this.setDefaultValue("mailer", "sender-addr", "clod-mc@glob.com.au");
    this.setDefaultValue("mailer", "admin-addr", "byron@glob.com.au");
  }

  public void setDefaultValue(@NotNull String section, @NotNull String name, int value) {
    if (!this.iniFile.hasKey(section, name)) {
      this.iniFile.setInteger(section, name, value);
    }
  }

  public void setDefaultValue(
      @NotNull String section, @NotNull String name, @NotNull String value) {
    if (!this.iniFile.hasKey(section, name)) {
      this.iniFile.setString(section, name, value);
    }
  }

  public int getInteger(@NotNull String section, @NotNull String name) {
    return this.iniFile.getInteger(section, name, 0);
  }

  public @NotNull String getString(@NotNull String section, @NotNull String name) {
    return this.iniFile.getString(section, name, "");
  }

  public @Nullable PlayerConfig getPlayerConfig(@NotNull Player player) {
    return this.playerConfigs.get(player.getName());
  }

  @EventHandler
  public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
    this.onPlayerJoin(event.getPlayer());
  }

  private void onPlayerJoin(@NotNull Player player) {
    assert this.playerConfigPath != null;
    this.playerConfigs.put(player.getName(), new PlayerConfig(this.playerConfigPath, player));
  }

  @EventHandler
  public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
    this.playerConfigs.remove(event.getPlayer().getName());
  }
}
