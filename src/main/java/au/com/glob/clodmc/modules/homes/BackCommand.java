package au.com.glob.clodmc.modules.homes;

import au.com.glob.clodmc.command.CommandError;
import au.com.glob.clodmc.command.CommandUtil;
import au.com.glob.clodmc.config.PlayerConfig;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BackCommand {
  public static void register() {
    new CommandAPICommand("back")
        .withShortDescription("Teleport to previous location")
        .withRequirement((sender) -> sender instanceof Player)
        .executes(
            (CommandSender sender, CommandArguments args) -> {
              try {
                Player player = CommandUtil.senderToPlayer(sender);
                PlayerConfig playerConfig = CommandUtil.getPlayerConfig(player);

                Location location = playerConfig.getBackLocation();
                if (location == null) {
                  throw new CommandError("No previous location");
                }

                player.sendRichMessage("<grey>Teleporting you back</grey>");
                playerConfig.setBackLocation(player.getLocation());
                player.teleportAsync(location);

              } catch (CommandError e) {
                sender.sendRichMessage("<red>" + e.getMessage() + "</red>");
              }
            })
        .register();
  }
}
