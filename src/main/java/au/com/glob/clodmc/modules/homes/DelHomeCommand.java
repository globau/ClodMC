package au.com.glob.clodmc.modules.homes;

import au.com.glob.clodmc.command.CommandError;
import au.com.glob.clodmc.command.CommandUtil;
import au.com.glob.clodmc.config.PlayerConfig;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DelHomeCommand {
  public static void register() {
    new CommandAPICommand("delhome")
        .withShortDescription("Delete home")
        .withRequirement((sender) -> sender instanceof Player)
        .withOptionalArguments(Homes.homesArgument("name"))
        .executes(
            (CommandSender sender, CommandArguments args) -> {
              try {
                Player player = CommandUtil.senderToPlayer(sender);
                PlayerConfig playerConfig = CommandUtil.getPlayerConfig(player);

                String name = (String) args.getOrDefault("name", PlayerConfig.DEFAULT_NAME);

                playerConfig.deleteHome(name);
                player.sendRichMessage("<grey>Home '" + name + "' deleted</grey>");

              } catch (CommandError e) {
                sender.sendRichMessage("<red>" + e.getMessage() + "</red>");
              }
            })
        .register();
  }
}
