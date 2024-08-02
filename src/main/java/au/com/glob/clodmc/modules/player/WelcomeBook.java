package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.CommandError;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.SimpleCommand;
import au.com.glob.clodmc.util.Chat;
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

/** Give players the Welcome Book, with rules and customisations */
public class WelcomeBook extends SimpleCommand implements Module, Listener {
  private static final @NotNull String TITLE = "Welcome to Clod-MC";
  private static final @NotNull String AUTHOR = "glob";
  private static final @NotNull List<String> PAGES =
      List.of(
          """
              Welcome to <b>Clod-MC</b>

              Map

                <click:open_url:https://clod.glob.au><blue>clod.glob.au</blue></click>

              Admin

                clod@glob.au""",
          """
              <b>Rules</b>

              Be respectful

              No Griefing
              No Stealing

              Quality of life
              client mods only""",
          """
              <b>AFK</b>

              Players idle for 5 mins will be marked as AFK

              <dark_green>/afk</dark_green>
               go afk now, or return from afk""",
          """
              <b>Homes</b>

              <dark_green>/sethome [name]</dark_green>
               set home loc
              <dark_green>/home [name]</dark_green>
               tp home
              <dark_green>/delhome [name]</dark_green>
               delete home
              <dark_green>/homes</dark_green>
              list homes
              <dark_green>/back</dark_green>
               tp to last loc
              <dark_green>/spawn</dark_green>
               tp to spawn""",
          """
              <b>Land Claims</b>

              Claim land with a golden shovel.

              <click:open_url:https://bit.ly/mcgpuser><blue>bit.ly/mcgpuser</blue></click>""",
          """
              <b>Invite Players</b>

              If you've played on the server long enough you can invite other players.

              <dark_green>/invite java {name}</dark_green>
               invite java player
              <dark_green>/invite bedrock {name}</dark_green>
               invite bedrock player""",
          """
              <b>Gateways</b>

              End-game player-built gateways

              <click:open_url:https://s.glob.au/wWw><blue>s.glob.au/wWw</blue></click>

              Name an anchor in an anvil to make it show up on the map.""",
          """
              <b>Other</b>

              Shift+Right-clicking an container's inventory will sort it.

              A named container will be visible when looking at it.""");

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
      @NotNull CommandSender sender, @NotNull String alias, String @NotNull [] args)
      throws IllegalArgumentException {
    String arg = args.length == 0 ? "" : args[0].toLowerCase();
    return Bukkit.getOnlinePlayers().stream()
        .map(Player::getName)
        .filter((String name) -> name.toLowerCase().startsWith(arg))
        .toList();
  }

  private void giveWelcomeBook(@NotNull Player recipient, @Nullable CommandSender sender) {
    ItemStack bookItem = new ItemStack(Material.WRITTEN_BOOK);
    BookMeta bookMeta = (BookMeta) bookItem.getItemMeta();

    BookMeta.BookMetaBuilder builder = bookMeta.toBuilder();
    for (String page : PAGES) {
      builder.addPage(MiniMessage.miniMessage().deserialize(page));
    }
    builder.title(Component.text(TITLE)).author(Component.text(AUTHOR));

    bookItem.setItemMeta(builder.build());

    recipient.getInventory().addItem(bookItem);
    if (sender != null) {
      Chat.fyi(sender, "Gave welcome book to " + recipient.getName());
    }
  }
}
