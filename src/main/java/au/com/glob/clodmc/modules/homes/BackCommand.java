package au.com.glob.clodmc.modules.homes;

import au.com.glob.clodmc.modules.CommandError;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.SimpleCommand;
import au.com.glob.clodmc.util.PlayerLocation;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BackCommand extends SimpleCommand implements Module {
  public BackCommand() {
    super("back", "Teleport to previous location");
  }

  @Override
  protected void execute(@NotNull CommandSender sender, @NotNull List<String> args) {
    Player player = this.toPlayer(sender);

    PlayerLocation location = Homes.instance.getBackLocation(player);
    if (location == null) {
      throw new CommandError("No previous location");
    }

    Homes.instance.setBackLocation(player);

    player.sendRichMessage("<grey>Teleporting you back</grey>");
    location.teleportPlayer(player);
  }
}
