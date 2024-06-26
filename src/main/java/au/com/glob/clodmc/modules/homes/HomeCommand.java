package au.com.glob.clodmc.modules.homes;

import au.com.glob.clodmc.modules.CommandError;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.SimpleCommand;
import au.com.glob.clodmc.util.PlayerLocation;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HomeCommand extends SimpleCommand implements Module {
  public HomeCommand() {
    super("home", "/home [name]", "Teleport home");
  }

  @Override
  protected void execute(@NotNull CommandSender sender, @NotNull List<String> args) {
    Player player = this.toPlayer(sender);
    String name = this.popArg(args, "home");

    Map<String, Location> homes = Homes.instance.getHomes(player);
    if (homes.isEmpty() || !homes.containsKey(name)) {
      throw new CommandError(name.equals("home") ? "No home set" : "No such home '" + name + "'");
    }

    Homes.instance.setBackLocation(player);

    player.sendRichMessage(
        "<grey>Teleporting you "
            + (name.equals("home") ? "home" : "to '" + name + "'")
            + "</grey>");
    PlayerLocation playerLoc = PlayerLocation.of(homes.get(name));
    playerLoc.teleportPlayer(player);
  }

  @Override
  public @NotNull List<String> tabComplete(
      @NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args)
      throws IllegalArgumentException {
    Player player = this.toPlayer(sender);
    if (args.length == 0) {
      return List.of();
    }

    Map<String, Location> homes = Homes.instance.getHomes(player);
    return homes.keySet().stream()
        .filter((String name) -> name.startsWith(args[0]))
        .sorted(String::compareToIgnoreCase)
        .toList();
  }
}
