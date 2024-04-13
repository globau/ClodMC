package au.com.glob.clodmc.modules.homes;

import au.com.glob.clodmc.command.CommandUtil;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class HomesCommand {
  public static void register() {
    new CommandAPICommand("homes")
        .withShortDescription("List homes")
        .withRequirement((sender) -> sender instanceof Player)
        .executes(
            (CommandSender sender, CommandArguments args) -> {
              Player player = CommandUtil.senderToPlayer(sender);
              FileConfiguration config = Homes.instance.getConfig(player);
              ConfigurationSection section = config.getConfigurationSection("homes");
              Collection<String> names = section == null ? List.of() : section.getKeys(false);

              if (names.isEmpty()) {
                player.sendRichMessage("Homes: <italic>None</italic>");
              } else {
                StringJoiner joiner = new StringJoiner(", ");
                for (String name : names.stream().sorted().toList()) {
                  joiner.add(name);
                }
                player.sendMessage("Homes: " + joiner);
              }
            })
        .register();
  }
}
