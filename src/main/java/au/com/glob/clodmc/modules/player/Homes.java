package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.command.CommandBuilder;
import au.com.glob.clodmc.command.CommandError;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Chat;
import au.com.glob.clodmc.util.PlayerDataFile;
import au.com.glob.clodmc.util.PlayerDataUpdater;
import au.com.glob.clodmc.util.PlayerLocation;
import au.com.glob.clodmc.util.TeleportUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Set limited number of named teleport locations */
public class Homes implements Listener, Module {
  protected static final int MAX_HOMES = 3;

  public Homes() {
    CommandBuilder.build("home")
        .usage("/home [name]")
        .description("Teleport home")
        .executor(
            (@NotNull Player player, @Nullable String name) -> {
              name = name == null ? "home" : name;

              Map<String, PlayerLocation> homes = this.getHomes(player);
              if (homes.isEmpty() || !homes.containsKey(name)) {
                throw new CommandError(
                    name.equals("home") ? "No home set" : "No such home '" + name + "'");
              }

              Chat.fyi(
                  player,
                  "Teleporting you " + (name.equals("home") ? "home" : "to '" + name + "'"));
              PlayerLocation playerLoc = homes.get(name);
              playerLoc.teleportPlayer(player);
            })
        .completor(
            (@NotNull Player player, @NotNull List<String> args) ->
                this.completeHomes(player, args))
        .register();

    CommandBuilder.build("homes")
        .description("List homes")
        .executor(
            (@NotNull Player player) -> {
              Map<String, PlayerLocation> homes = this.getHomes(player);

              if (homes.isEmpty()) {
                Chat.warning(player, "No homes");
              } else {
                StringJoiner joiner = new StringJoiner(", ");
                for (String name : homes.keySet().stream().sorted().toList()) {
                  joiner.add(name);
                }
                Chat.info(player, "Homes: " + joiner);
              }
            })
        .register();

    CommandBuilder.build("sethome")
        .usage("/sethome [name]")
        .description("Sets a home to your current location")
        .executor(
            (@NotNull Player player, @Nullable String name) -> {
              name = name == null ? "home" : name;

              Map<String, PlayerLocation> homes = this.getHomes(player);
              boolean existing = homes.containsKey(name);

              if (!existing && homes.size() >= MAX_HOMES) {
                throw new CommandError(
                    "You have reached the maximum number of homes (" + MAX_HOMES + ")");
              }

              Location location = TeleportUtil.getStandingPos(player);

              if (TeleportUtil.isUnsafe(location.getBlock())) {
                throw new CommandError("Your current location is not safe");
              }

              homes.put(name, PlayerLocation.of(location));
              this.setHomes(player, homes);

              if (name.equals("home")) {
                Chat.info(
                    player, "Home " + (existing ? "updated" : "set") + " to you current location");
              } else {
                Chat.info(player, "Home '" + name + "' " + (existing ? "updated" : "created"));
              }
            })
        .register();

    CommandBuilder.build("delhome")
        .usage("/delhome [name]")
        .description("Delete home")
        .executor(
            (@NotNull Player player, @Nullable String name) -> {
              name = name == null ? "home" : name;

              Map<String, PlayerLocation> homes = this.getHomes(player);
              if (homes.isEmpty() || !homes.containsKey(name)) {
                throw new CommandError(
                    name.equals("home") ? "No home set" : "No such home '" + name + "'");
              }

              homes.remove(name);
              this.setHomes(player, homes);

              if (name.equals("home")) {
                Chat.info(player, "Deleted home");
              } else {
                Chat.info(player, "Deleted home '" + name + "'");
              }
            })
        .completor(
            (@NotNull Player player, @NotNull List<String> args) ->
                this.completeHomes(player, args))
        .register();
  }

  private @NotNull List<String> completeHomes(@NotNull Player player, @NotNull List<String> args) {
    if (args.isEmpty()) {
      return List.of();
    }

    Map<String, PlayerLocation> homes = this.getHomes(player);
    return homes.keySet().stream()
        .filter((String name) -> name.startsWith(args.getFirst()))
        .sorted(String::compareToIgnoreCase)
        .toList();
  }

  private @NotNull Map<String, PlayerLocation> getHomes(@NotNull Player player) {
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

  private void setHomes(@NotNull Player player, @NotNull Map<String, PlayerLocation> homes) {
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
}
