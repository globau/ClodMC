package au.com.glob.clodmc.modules.homes;

import au.com.glob.clodmc.command.CommandError;
import au.com.glob.clodmc.command.SimpleCommand;
import au.com.glob.clodmc.util.BlockPos;
import au.com.glob.clodmc.util.PlayerLocation;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BackCommand extends SimpleCommand {
  public static void register() {
    SimpleCommand.register(new BackCommand());
  }

  protected BackCommand() {
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
    try {
      location.teleportPlayer(player);
    } catch (BlockPos.LocationError e) {
      throw new CommandError(e.getMessage());
    }
  }
}
