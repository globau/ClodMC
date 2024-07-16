package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.config.Config;
import au.com.glob.clodmc.modules.CommandError;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.SimpleCommand;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WelcomeBook extends SimpleCommand implements Module, Listener {
  private static final @NotNull String FILENAME = "welcome-book.yml";

  public WelcomeBook() {
    super("welcome", "/welcome <player>", "Give specified player the welcome book");
  }

  @EventHandler
  public void onJoin(@NotNull PlayerJoinEvent event) {
    new BukkitRunnable() {
      @Override
      public void run() {
        if (!event.getPlayer().hasPlayedBefore()) {
          WelcomeBook.this.giveWelcomeBook(event.getPlayer(), null);
        }
      }
    }.runTaskLater(ClodMC.instance, 5);
  }

  @Override
  protected void execute(@NotNull CommandSender sender, @NotNull List<String> args) {
    if (sender instanceof Player player && !player.isOp()) {
      throw new CommandError("You are not allowed to give players welcome books");
    }

    this.giveWelcomeBook(this.popPlayerArg(args), sender);
  }

  @Override
  public @NotNull List<String> tabComplete(
      @NotNull CommandSender sender, @NotNull String alias, @NotNull String @NotNull [] args)
      throws IllegalArgumentException {
    String arg = args.length == 0 ? "" : args[0].toLowerCase();
    return Bukkit.getOnlinePlayers().stream()
        .map(Player::getName)
        .filter((String name) -> name.toLowerCase().startsWith(arg))
        .toList();
  }

  private void giveWelcomeBook(@NotNull Player recipient, @Nullable CommandSender sender) {
    Config config = Config.of(FILENAME);
    try {
      List<String> pages = config.getStringList("pages");
      if (pages.isEmpty()) {
        ClodMC.logError(FILENAME + " is empty");
        return;
      }

      ItemStack bookItem = new ItemStack(Material.WRITTEN_BOOK);
      BookMeta bookMeta = (BookMeta) bookItem.getItemMeta();

      BookMeta.BookMetaBuilder builder = bookMeta.toBuilder();
      for (String page : pages) {
        builder.addPage(MiniMessage.miniMessage().deserialize(page));
      }
      builder
          .title(Component.text(config.getString("title", "")))
          .author(Component.text(config.getString("author", "")));

      bookItem.setItemMeta(builder.build());

      recipient.getInventory().addItem(bookItem);
      if (sender != null) {
        ClodMC.fyi(sender, "Gave welcome book to " + recipient.getName());
      }
    } finally {
      Config.unload(FILENAME);
    }
  }
}
