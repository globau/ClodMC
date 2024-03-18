package au.com.glob.homes.config;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerConfig {
  public static final String DEFAULT_NAME = "home";
  public static final String BACK_NAME = "_back";

  private final Player player;
  private final File configFile;
  private final Map<String, Location> homes = new HashMap<>();
  private Location backLocation;

  public PlayerConfig(@NotNull File path, @NotNull Player player) {
    this.player = player;
    this.configFile = new File(path, player.getUniqueId() + ".yml");

    ConfigFile config = new ConfigFile(this.configFile, this.player.getName());
    for (Map.Entry<String, String> entrySet : config.entrySet()) {
      String name = entrySet.getKey();
      Location location = this.stringToLocation(entrySet.getValue());
      if (location == null) {
        continue;
      }
      if (name.equals(BACK_NAME)) {
        this.backLocation = location;
      } else if (this.homes.size() <= PluginConfig.getInstance().getMaxHomes()) {
        this.homes.put(name, location);
      }
    }
  }

  private void save() {
    ConfigFile config = new ConfigFile(this.configFile, this.player.getName());
    config.set(BACK_NAME, this.locationToString(this.backLocation));
    for (Map.Entry<String, Location> entrySet : this.homes.entrySet()) {
      config.set(entrySet.getKey(), this.locationToString(entrySet.getValue()));
    }
    config.save();
  }

  public @Nullable Location getHome(@NotNull String name) {
    if (name.equals(BACK_NAME)) {
      return this.backLocation;
    } else {
      return this.homes.get(name.toLowerCase(Locale.ENGLISH));
    }
  }

  public @NotNull List<String> getHomes() {
    ArrayList<String> result = new ArrayList<>(this.homes.keySet());
    result.sort(String.CASE_INSENSITIVE_ORDER);
    return result;
  }

  public void setHome(@NotNull String name, @Nullable Location location) {
    if (name.equals(BACK_NAME)) {
      this.backLocation = location;
    } else {
      this.homes.put(name, location);
    }
    this.save();
  }

  private @Nullable String locationToString(@Nullable Location location) {
    if (location == null) {
      return null;
    }
    StringJoiner result = new StringJoiner(" ");
    for (Map.Entry<String, Object> entry : location.serialize().entrySet()) {
      String s = entry.getKey() + ":" + entry.getValue();
      result.add(s);
    }
    return result.toString();
  }

  private @Nullable Location stringToLocation(@NotNull String value) {
    Map<String, Object> dict = new HashMap<>();
    for (String pair : value.split("\\s+")) {
      String[] parts = pair.split(":", 2);
      if (parts.length != 2) {
        return null;
      }
      dict.put(parts[0], parts[1]);
    }
    return Location.deserialize(dict);
  }
}
