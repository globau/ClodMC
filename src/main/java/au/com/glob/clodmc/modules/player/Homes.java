package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.CommandError;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.SimpleCommand;
import au.com.glob.clodmc.util.PlayerDataFile;
import au.com.glob.clodmc.util.PlayerDataUpdater;
import au.com.glob.clodmc.util.PlayerLocation;
import au.com.glob.clodmc.util.TeleportUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class Homes implements Listener, Module {
  @SuppressWarnings("NotNullFieldNotInitialized")
  protected static @NotNull Homes instance;

  protected static final int MAX_HOMES = 3;

  public Homes() {
    instance = this;
  }

  @Override
  public @NotNull List<? extends SimpleCommand> getCommands() {
    return List.of(
        new DelHomeCommand(), new HomeCommand(), new HomesCommand(), new SetHomeCommand());
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

  //

  private static @NotNull List<String> tabComplete(
      @NotNull Player player, String @NotNull [] args) {
    if (args.length == 0) {
      return List.of();
    }

    Map<String, PlayerLocation> homes = instance.getHomes(player);
    return homes.keySet().stream()
        .filter((String name) -> name.startsWith(args[0]))
        .sorted(String::compareToIgnoreCase)
        .toList();
  }

  private static class DelHomeCommand extends SimpleCommand {
    public DelHomeCommand() {
      super("delhome", "/delhome [name]", "Delete home");
    }

    @Override
    protected void execute(@NotNull CommandSender sender, @NotNull List<String> args) {
      Player player = this.toPlayer(sender);
      String name = this.popArg(args, "home");

      Map<String, PlayerLocation> homes = instance.getHomes(player);
      if (homes.isEmpty() || !homes.containsKey(name)) {
        throw new CommandError(name.equals("home") ? "No home set" : "No such home '" + name + "'");
      }

      homes.remove(name);
      instance.setHomes(player, homes);

      if (name.equals("home")) {
        ClodMC.info(player, "Deleted home");
      } else {
        ClodMC.info(player, "Deleted home '" + name + "'");
      }
    }

    @Override
    public @NotNull List<String> tabComplete(
        @NotNull CommandSender sender, @NotNull String alias, String @NotNull [] args)
        throws IllegalArgumentException {
      return Homes.tabComplete(this.toPlayer(sender), args);
    }
  }

  private static class HomeCommand extends SimpleCommand {
    public HomeCommand() {
      super("home", "/home [name]", "Teleport home");
    }

    @Override
    protected void execute(@NotNull CommandSender sender, @NotNull List<String> args) {
      Player player = this.toPlayer(sender);
      String name = this.popArg(args, "home");

      Map<String, PlayerLocation> homes = instance.getHomes(player);
      if (homes.isEmpty() || !homes.containsKey(name)) {
        throw new CommandError(name.equals("home") ? "No home set" : "No such home '" + name + "'");
      }

      ClodMC.fyi(player, "Teleporting you " + (name.equals("home") ? "home" : "to '" + name + "'"));
      PlayerLocation playerLoc = homes.get(name);
      playerLoc.teleportPlayer(player);
    }

    @Override
    public @NotNull List<String> tabComplete(
        @NotNull CommandSender sender, @NotNull String alias, String @NotNull [] args)
        throws IllegalArgumentException {
      return Homes.tabComplete(this.toPlayer(sender), args);
    }
  }

  private static class HomesCommand extends SimpleCommand {
    public HomesCommand() {
      super("homes", "List homes");
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull List<String> args) {
      Player player = this.toPlayer(sender);
      Map<String, PlayerLocation> homes = instance.getHomes(player);

      if (homes.isEmpty()) {
        ClodMC.warning(player, "No homes");
      } else {
        StringJoiner joiner = new StringJoiner(", ");
        for (String name : homes.keySet().stream().sorted().toList()) {
          joiner.add(name);
        }
        ClodMC.info(player, "Homes: " + joiner);
      }
    }
  }

  private static class SetHomeCommand extends SimpleCommand {
    public SetHomeCommand() {
      super("sethome", "/sethome [name]", "Sets a home to your current location");
    }

    @Override
    protected void execute(@NotNull CommandSender sender, @NotNull List<String> args) {
      Player player = this.toPlayer(sender);
      String name = this.popArg(args, "home");

      Map<String, PlayerLocation> homes = instance.getHomes(player);
      boolean existing = homes.containsKey(name);

      if (!existing && homes.size() >= MAX_HOMES) {
        throw new CommandError("You have reached the maximum number of homes (" + MAX_HOMES + ")");
      }

      if (TeleportUtil.isUnsafe(player.getLocation().getBlock(), false)) {
        throw new CommandError("Your current location is not safe");
      }

      homes.put(name, PlayerLocation.of(player));
      instance.setHomes(player, homes);

      if (name.equals("home")) {
        ClodMC.info(player, "Home " + (existing ? "updated" : "set") + " to you current location");
      } else {
        ClodMC.info(player, "Home '" + name + "' " + (existing ? "updated" : "created"));
      }
    }
  }
}
