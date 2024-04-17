package au.com.glob.clodmc.modules.welcome;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class WelcomeCommand {
  public static void register() {
    // /welcome {player} --> gives them the welcome book
    new CommandAPICommand("welcome")
        .withShortDescription("Give specified play the welcome book")
        .withPermission(CommandPermission.OP)
        .withArguments(new PlayerArgument("player"))
        .executes(
            (CommandSender sender, CommandArguments args) -> {
              Player player = (Player) args.get("player");
              ItemStack book = WelcomeBook.build();
              if (player != null && book != null) {
                player.getInventory().addItem(book);
                sender.sendRichMessage("Gave welcome book to " + player.getName());
              }
            })
        .register();
  }
}
