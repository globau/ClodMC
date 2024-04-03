package au.com.glob.clodmc.modules.homes;

import au.com.glob.clodmc.command.CommandError;
import au.com.glob.clodmc.command.CommandUtil;
import au.com.glob.clodmc.config.PlayerConfig;
import au.com.glob.clodmc.config.PluginConfig;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetHomeCommand {
  public static void register() {
    new CommandAPICommand("sethome")
        .withShortDescription("Sets a home to your current location")
        .withRequirement((sender) -> sender instanceof Player)
        .withOptionalArguments(Homes.homesArgument("name"))
        .executes(
            (CommandSender sender, CommandArguments args) -> {
              try {
                Player player = CommandUtil.senderToPlayer(sender);
                PlayerConfig playerConfig = CommandUtil.getPlayerConfig(player);

                String name = (String) args.getOrDefault("name", PlayerConfig.DEFAULT_NAME);
                int maxHomes = PluginConfig.getInstance().getInteger("homes", "max-allowed");

                boolean existing = playerConfig.getHomeLocation(name) != null;
                if (!existing && playerConfig.getHomeNames().size() >= maxHomes) {
                  throw new CommandError(
                      "You have reached the maximum number of homes (" + maxHomes + ")");
                }

                playerConfig.setHomeLocation(name, player.getLocation());
                if (name.equals(PlayerConfig.DEFAULT_NAME)) {
                  player.sendMessage(
                      "Home " + (existing ? "updated" : "set") + " to you current location");
                } else {
                  player.sendMessage("Home '" + name + "' " + (existing ? "updated" : "created"));
                }

              } catch (CommandError e) {
                sender.sendRichMessage("<red>" + e.getMessage() + "</red>");
              }
            })
        .register();
  }
}
