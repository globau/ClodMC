package au.com.glob.clodmc.modules.homes;

import au.com.glob.clodmc.command.CommandError;
import au.com.glob.clodmc.command.CommandUtil;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
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
                String name = (String) args.getOrDefault("name", "home");

                FileConfiguration config = Homes.instance.getConfig(player);
                config.set("homes." + name, null);
                Homes.instance.saveConfig(player, config);

                player.sendRichMessage("<grey>Home '" + name + "' deleted</grey>");

              } catch (CommandError e) {
                sender.sendRichMessage("<red>" + e.getMessage() + "</red>");
              }
            })
        .register();
  }
}
