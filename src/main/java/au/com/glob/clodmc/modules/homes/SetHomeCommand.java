package au.com.glob.clodmc.modules.homes;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.command.CommandError;
import au.com.glob.clodmc.command.SimpleCommand;
import au.com.glob.clodmc.util.BlockPos;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SetHomeCommand extends SimpleCommand {
  public static void register() {
    SimpleCommand.register(new SetHomeCommand());
  }

  protected SetHomeCommand() {
    super("sethome", "/sethome [name]", "Sets a home to your current location");
  }

  @Override
  protected void execute(@NotNull CommandSender sender, @NotNull List<String> args) {
    Player player = this.toPlayer(sender);
    String name = this.popArg(args, "home");

    int maxHomes = ClodMC.instance.getConfig().getInt("homes.max-allowed");

    Map<String, Location> homes = Homes.instance.getHomes(player);
    boolean existing = homes.containsKey(name);

    if (homes.size() >= maxHomes) {
      throw new CommandError("You have reached the maximum number of homes (" + maxHomes + ")");
    }

    if (BlockPos.of(player.getLocation()).isUnsafe()) {
      throw new CommandError("Your current location is not safe");
    }

    homes.put(name, player.getLocation());
    Homes.instance.setHomes(player, homes);

    if (name.equals("home")) {
      player.sendMessage("Home " + (existing ? "updated" : "set") + " to you current location");
    } else {
      player.sendMessage("Home '" + name + "' " + (existing ? "updated" : "created"));
    }
  }
}
