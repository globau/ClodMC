package au.com.glob.clodmc.modules.homes;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.command.CommandError;
import au.com.glob.clodmc.command.CommandUtil;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
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
                int maxHomes = ClodMC.instance.getConfig().getInt("homes.max-allowed");
                Player player = CommandUtil.senderToPlayer(sender);
                String name = (String) args.getOrDefault("name", "home");

                FileConfiguration config = Homes.instance.getConfig(player);
                ConfigurationSection section = config.getConfigurationSection("homes");
                boolean existing = section != null && section.contains(name);

                if (section != null && !existing && section.getKeys(false).size() >= maxHomes) {
                  throw new CommandError(
                      "You have reached the maximum number of homes (" + maxHomes + ")");
                }

                config.set("homes." + name, player.getLocation());
                Homes.instance.saveConfig(player, config);
                if (name.equals("home")) {
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
