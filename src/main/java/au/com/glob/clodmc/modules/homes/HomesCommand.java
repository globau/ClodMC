package au.com.glob.clodmc.modules.homes;

import au.com.glob.clodmc.command.CommandError;
import au.com.glob.clodmc.command.CommandUtil;
import au.com.glob.clodmc.config.PlayerConfig;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import java.util.List;
import java.util.StringJoiner;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HomesCommand {
  public static void register() {
    new CommandAPICommand("homes")
        .withShortDescription("List homes")
        .withRequirement((sender) -> sender instanceof Player)
        .executes(
            (CommandSender sender, CommandArguments args) -> {
              try {
                Player player = CommandUtil.senderToPlayer(sender);
                PlayerConfig playerConfig = CommandUtil.getPlayerConfig(player);
                List<String> names = playerConfig.getHomeNames();

                if (names.isEmpty()) {
                  player.sendRichMessage("Homes: <italic>None</italic>");
                } else {
                  StringJoiner joiner = new StringJoiner(", ");
                  for (String name : names) {
                    joiner.add(name);
                  }
                  player.sendMessage("Homes: " + joiner);
                }

              } catch (CommandError e) {
                sender.sendRichMessage("<red>" + e.getMessage() + "</red>");
              }
            })
        .register();
  }
}
