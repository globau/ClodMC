package au.com.glob.clodmc.config;

import au.com.glob.clodmc.IniFile;
import java.io.File;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerConfig {
  public static final String DEFAULT_NAME = "home";

  private final IniFile iniFile;

  public PlayerConfig(@NotNull File path, @NotNull Player player) {
    File configFile = new File(path, player.getUniqueId().toString());
    this.iniFile = new IniFile(configFile, player.getName());
  }

  public @Nullable Location getHomeLocation(@NotNull String name) {
    return this.iniFile.getLocation("homes", name, null);
  }

  public void setHomeLocation(@NotNull String name, @NotNull Location location) {
    this.iniFile.setLocation("homes", name, location);
  }

  public void deleteHome(@NotNull String name) {
    this.iniFile.remove("homes", name);
  }

  public @NotNull List<String> getHomeNames() {
    return this.iniFile.getKeys("homes");
  }

  public @Nullable Location getBackLocation() {
    return this.iniFile.getLocation("homes-unlisted", "back", null);
  }

  public void setBackLocation(@NotNull Location location) {
    this.iniFile.setLocation("homes-unlisted", "back", location);
  }
}
