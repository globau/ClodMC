package au.com.glob.clodmc.modules.homes;

import au.com.glob.clodmc.command.CommandError;
import au.com.glob.clodmc.command.CommandUtil;
import au.com.glob.clodmc.config.PlayerConfig;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HomeCommand {
  public static void register() {
    new CommandAPICommand("home")
        .withShortDescription("Teleport home")
        .withRequirement((sender) -> sender instanceof Player)
        .withOptionalArguments(Homes.homesArgument("name"))
        .executes(
            (CommandSender sender, CommandArguments args) -> {
              try {
                Player player = CommandUtil.senderToPlayer(sender);
                PlayerConfig playerConfig = CommandUtil.getPlayerConfig(player);

                String name = (String) args.getOrDefault("name", PlayerConfig.DEFAULT_NAME);

                Location location = playerConfig.getHomeLocation(name);
                if (location == null) {
                  throw new CommandError(
                      name.equals(PlayerConfig.DEFAULT_NAME)
                          ? "No home set"
                          : "No such home '" + name + "'");
                }

                player.sendRichMessage(
                    "<grey>Teleporting you "
                        + (name.equals(PlayerConfig.DEFAULT_NAME) ? "home" : "to '" + name + "'")
                        + "</grey>");
                playerConfig.setBackLocation(player.getLocation());
                player.teleportAsync(location);

              } catch (CommandError e) {
                sender.sendRichMessage("<red>" + e.getMessage() + "</red>");
              }
            })
        .register();
  }
}
