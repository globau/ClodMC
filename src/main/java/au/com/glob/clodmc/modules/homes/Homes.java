package au.com.glob.clodmc.modules.homes;

import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.player.PlayerData;
import au.com.glob.clodmc.util.PlayerLocation;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Homes implements Listener, Module {
  protected static Homes instance;

  public Homes() {
    super();
    instance = this;
  }

  protected @NotNull Map<String, Location> getHomes(@NotNull Player player) {
    PlayerData.Config config = PlayerData.of(player);
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
    PlayerData.Config config = PlayerData.of(player);
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
    config.save();
  }

  protected void setBackLocation(@NotNull Player player) {
    PlayerLocation playerLocation = PlayerLocation.of(player);
    PlayerData.Config config = PlayerData.of(player);
    config.set("homes_internal.back", playerLocation);
    config.save();
  }

  protected @Nullable PlayerLocation getBackLocation(@NotNull Player player) {
    PlayerData.Config config = PlayerData.of(player);
    return (PlayerLocation) config.get("homes_internal.back");
  }
}
