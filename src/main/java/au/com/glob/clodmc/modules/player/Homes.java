package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.annotations.Audience;
import au.com.glob.clodmc.annotations.Doc;
import au.com.glob.clodmc.command.CommandBuilder;
import au.com.glob.clodmc.command.CommandError;
import au.com.glob.clodmc.datafile.PlayerDataFile;
import au.com.glob.clodmc.datafile.PlayerDataFiles;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Chat;
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
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@Doc(
    audience = Audience.PLAYER,
    title = "Homes",
    description = "Set limited number of named teleport locations")
@NullMarked
public class Homes implements Listener, Module {
  private static final int MAX_HOMES = 3;
  private static final String DEFAULT_NAME = "home";

  public Homes() {
    CommandBuilder.build("home")
        .usage("/home [name]")
        .description("Teleport home")
        .executor(
            (Player player, @Nullable String name) -> {
              name = name == null ? DEFAULT_NAME : name;

              Map<String, Location> homes = getHomes(player);
              if (homes.isEmpty() || !homes.containsKey(name)) {
                throw new CommandError(
                    name.equals(DEFAULT_NAME)
                        ? "No home set"
                        : "No such home '%s'".formatted(name));
              }

              Location location = homes.get(name);
              TeleportUtil.teleport(
                  player, location, name.equals(DEFAULT_NAME) ? "home" : "to '%s'".formatted(name));
            })
        .completor(Homes::completeHomes);

    CommandBuilder.build("homes")
        .description("List homes")
        .executor(
            (Player player) -> {
              Map<String, Location> homes = getHomes(player);

              if (homes.isEmpty()) {
                Chat.warning(player, "No homes");
              } else {
                StringJoiner joiner = new StringJoiner(", ");
                for (String name : homes.keySet().stream().sorted().toList()) {
                  joiner.add(name);
                }
                Chat.info(player, "Homes: %s".formatted(joiner));
              }
            });

    CommandBuilder.build("sethome")
        .usage("/sethome [name]")
        .description("Sets a home to your current location")
        .executor(
            (Player player, @Nullable String name) -> {
              name = name == null ? DEFAULT_NAME : name;

              Map<String, Location> homes = getHomes(player);
              boolean existing = homes.containsKey(name);

              if (!existing && homes.size() >= MAX_HOMES) {
                throw new CommandError(
                    "You have reached the maximum number of homes (%d)".formatted(MAX_HOMES));
              }

              Location location = TeleportUtil.getStandingPos(player);

              if (TeleportUtil.isUnsafe(location.getBlock())) {
                throw new CommandError("Your current location is not safe");
              }

              homes.put(name, location);
              setHomes(player, homes);

              if (name.equals(DEFAULT_NAME)) {
                Chat.info(
                    player,
                    "Home %s to you current location".formatted(existing ? "updated" : "set"));
              } else {
                Chat.info(player, "Home '%s' %s".formatted(name, existing ? "updated" : "created"));
              }
            });

    CommandBuilder.build("delhome")
        .usage("/delhome [name]")
        .description("Delete home")
        .executor(
            (Player player, @Nullable String name) -> {
              name = name == null ? DEFAULT_NAME : name;

              Map<String, Location> homes = getHomes(player);
              if (homes.isEmpty() || !homes.containsKey(name)) {
                throw new CommandError(
                    name.equals(DEFAULT_NAME)
                        ? "No home set"
                        : "No such home '%s'".formatted(name));
              }

              homes.remove(name);
              setHomes(player, homes);

              if (name.equals(DEFAULT_NAME)) {
                Chat.info(player, "Deleted home");
              } else {
                Chat.info(player, "Deleted home '%s'".formatted(name));
              }
            })
        .completor(Homes::completeHomes);
  }

  // tab completion for home names
  private static List<String> completeHomes(Player player, List<String> args) {
    if (args.isEmpty()) {
      return List.of();
    }

    Map<String, Location> homes = getHomes(player);
    return homes.keySet().stream()
        .filter((String name) -> name.startsWith(args.getFirst()))
        .sorted(String::compareToIgnoreCase)
        .toList();
  }

  // retrieve all homes for a player from data file
  private static Map<String, Location> getHomes(Player player) {
    PlayerDataFile dataFile = PlayerDataFiles.of(player);

    ConfigurationSection section = dataFile.getConfigurationSection("homes");
    if (section == null) {
      return new HashMap<>(0);
    }
    Map<String, Location> result = new HashMap<>();
    for (String name : section.getKeys(false)) {
      Location playerLocation =
          dataFile.getSerializable("homes.%s".formatted(name), Location.class, null);
      result.put(name, Objects.requireNonNull(playerLocation));
    }
    return result;
  }

  // save all homes for a player to data file
  private static void setHomes(Player player, Map<String, Location> homes) {
    PlayerDataFile dataFile = PlayerDataFiles.of(player);
    ConfigurationSection section = dataFile.getConfigurationSection("homes");
    if (section != null) {
      for (String name : section.getKeys(false)) {
        if (!homes.containsKey(name)) {
          section.set(name, null);
        }
      }
    }
    for (String name : homes.keySet()) {
      dataFile.set("homes.%s".formatted(name), homes.get(name));
    }
    dataFile.save();
  }
}
