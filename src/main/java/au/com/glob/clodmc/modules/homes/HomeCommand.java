package au.com.glob.clodmc.modules.homes;

import au.com.glob.clodmc.command.CommandError;
import au.com.glob.clodmc.command.CommandUtil;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
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
                String name = (String) args.getOrDefault("name", "home");

                FileConfiguration config = Homes.instance.getConfig(player);
                Location location = config.getLocation("homes." + name);
                if (location == null) {
                  throw new CommandError(
                      name.equals("home") ? "No home set" : "No such home '" + name + "'");
                }
                config.set("internal.back", player.getLocation());
                Homes.instance.saveConfig(player, config);

                player.sendRichMessage(
                    "<grey>Teleporting you "
                        + (name.equals("home") ? "home" : "to '" + name + "'")
                        + "</grey>");
                player.teleportAsync(location);

              } catch (CommandError e) {
                sender.sendRichMessage("<red>" + e.getMessage() + "</red>");
              }
            })
        .register();
  }
}
