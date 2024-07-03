package au.com.glob.clodmc.modules.homes;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.CommandError;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.SimpleCommand;
import au.com.glob.clodmc.util.TeleportUtil;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SetHomeCommand extends SimpleCommand implements Module {
  public SetHomeCommand() {
    super("sethome", "/sethome [name]", "Sets a home to your current location");
  }

  @Override
  protected void execute(@NotNull CommandSender sender, @NotNull List<String> args) {
    Player player = this.toPlayer(sender);
    String name = this.popArg(args, "home");

    int maxHomes = ClodMC.instance.getConfig().getInt("homes.max-allowed");

    Map<String, Location> homes = Homes.instance.getHomes(player);
    boolean existing = homes.containsKey(name);

    if (!existing && homes.size() >= maxHomes) {
      throw new CommandError("You have reached the maximum number of homes (" + maxHomes + ")");
    }

    if (TeleportUtil.isUnsafe(player.getLocation().getBlock(), false)) {
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
