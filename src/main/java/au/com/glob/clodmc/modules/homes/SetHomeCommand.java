package au.com.glob.clodmc.modules.homes;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.CommandError;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.SimpleCommand;
import au.com.glob.clodmc.util.PlayerLocation;
import au.com.glob.clodmc.util.TeleportUtil;
import java.util.List;
import java.util.Map;
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

    Map<String, PlayerLocation> homes = Homes.instance.getHomes(player);
    boolean existing = homes.containsKey(name);

    if (!existing && homes.size() >= Homes.MAX_HOMES) {
      throw new CommandError(
          "You have reached the maximum number of homes (" + Homes.MAX_HOMES + ")");
    }

    if (TeleportUtil.isUnsafe(player.getLocation().getBlock(), false)) {
      throw new CommandError("Your current location is not safe");
    }

    homes.put(name, PlayerLocation.of(player));
    Homes.instance.setHomes(player, homes);

    if (name.equals("home")) {
      ClodMC.info(player, "Home " + (existing ? "updated" : "set") + " to you current location");
    } else {
      ClodMC.info(player, "Home '" + name + "' " + (existing ? "updated" : "created"));
    }
  }
}
