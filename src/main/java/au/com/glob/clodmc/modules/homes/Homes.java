package au.com.glob.clodmc.modules.homes;

import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.PlayerDataFile;
import au.com.glob.clodmc.util.PlayerDataUpdater;
import au.com.glob.clodmc.util.PlayerLocation;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Homes implements Listener, Module {
  @SuppressWarnings("NotNullFieldNotInitialized")
  protected static @NotNull Homes instance;

  protected static final int MAX_HOMES = 3;
  protected static final @NotNull String OVERWORLD_NAME = "world";

  public Homes() {
    instance = this;
  }

  protected @NotNull Map<String, PlayerLocation> getHomes(@NotNull Player player) {
    PlayerDataFile config = PlayerDataFile.of(player);

    ConfigurationSection section = config.getConfigurationSection("homes");
    if (section == null) {
      return new HashMap<>(0);
    }
    Map<String, PlayerLocation> result = new HashMap<>();
    for (String name : section.getKeys(false)) {
      PlayerLocation playerLocation =
          config.getSerializable("homes." + name, PlayerLocation.class, null);
      result.put(name, Objects.requireNonNull(playerLocation));
    }
    return result;
  }

  protected void setHomes(@NotNull Player player, @NotNull Map<String, PlayerLocation> homes) {
    try (PlayerDataUpdater config = PlayerDataUpdater.of(player)) {
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
    }
  }

  protected void setBackLocation(@NotNull Player player) {
    try (PlayerDataUpdater config = PlayerDataUpdater.of(player)) {
      config.set("homes_internal.back", PlayerLocation.of(player));
    }
  }

  protected @Nullable PlayerLocation getBackLocation(@NotNull Player player) {
    PlayerDataFile config = PlayerDataFile.of(player);
    return (PlayerLocation) config.get("homes_internal.back");
  }
}
