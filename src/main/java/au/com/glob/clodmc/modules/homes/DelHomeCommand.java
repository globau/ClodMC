package au.com.glob.clodmc.modules.homes;

import au.com.glob.clodmc.modules.CommandError;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.SimpleCommand;
import au.com.glob.clodmc.util.PlayerLocation;
import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DelHomeCommand extends SimpleCommand implements Module {
  public DelHomeCommand() {
    super("delhome", "/delhome [name]", "Delete home");
  }

  @Override
  protected void execute(@NotNull CommandSender sender, @NotNull List<String> args) {
    Player player = this.toPlayer(sender);
    String name = this.popArg(args, "home");

    Map<String, PlayerLocation> homes = Homes.instance.getHomes(player);
    if (homes.isEmpty() || !homes.containsKey(name)) {
      throw new CommandError(name.equals("home") ? "No home set" : "No such home '" + name + "'");
    }

    homes.remove(name);
    Homes.instance.setHomes(player, homes);

    if (name.equals("home")) {
      player.sendMessage("Deleted home");
    } else {
      player.sendMessage("Deleted home '" + name + "'");
    }
  }

  @Override
  public @NotNull List<String> tabComplete(
      @NotNull CommandSender sender, @NotNull String alias, @NotNull String @NotNull [] args)
      throws IllegalArgumentException {
    Player player = this.toPlayer(sender);
    if (args.length == 0) {
      return List.of();
    }

    Map<String, PlayerLocation> homes = Homes.instance.getHomes(player);
    return homes.keySet().stream().sorted(String::compareToIgnoreCase).toList();
  }
}
