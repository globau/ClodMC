package au.com.glob.clodmc.modules.homes;

import au.com.glob.clodmc.command.SimpleCommand;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DelHomeCommand extends SimpleCommand {
  public static void register() {
    SimpleCommand.register(new DelHomeCommand());
  }

  protected DelHomeCommand() {
    super("delhome", "/delhome [name]", "Delete home");
  }

  @Override
  protected void execute(@NotNull CommandSender sender, @NotNull List<String> args) {
    Player player = this.toPlayer(sender);
    String name = this.popArg(args, "home");

    Map<String, Location> homes = Homes.instance.getHomes(player);
    homes.remove(name);
    Homes.instance.setHomes(player, homes);
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
    return homes.keySet().stream().sorted(String::compareToIgnoreCase).toList();
  }
}
