package au.com.glob.clodmc.modules.welcome;

import au.com.glob.clodmc.modules.CommandError;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.SimpleCommand;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class WelcomeCommand extends SimpleCommand implements Module {
  public WelcomeCommand() {
    super("welcome", "/welcome <player>", "Give specified player the welcome book");
  }

  @Override
  protected void execute(@NotNull CommandSender sender, @NotNull List<String> args) {
    if (sender instanceof Player player && !player.isOp()) {
      throw new CommandError("You are not allowed to give players welcome books");
    }

    Player player = this.popPlayerArg(args);
    ItemStack book = WelcomeBook.build();
    if (book != null) {
      player.getInventory().addItem(book);
      sender.sendRichMessage("Gave welcome book to " + player.getName());
    }
  }

  @Override
  public @NotNull List<String> tabComplete(
      @NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args)
      throws IllegalArgumentException {
    String arg = args.length == 0 ? "" : args[0].toLowerCase();
    return Bukkit.getOnlinePlayers().stream()
        .map(Player::getName)
        .map(String::toLowerCase)
        .filter((String name) -> name.startsWith(arg))
        .toList();
  }
}
