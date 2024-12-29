package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.command.CommandBuilder;
import au.com.glob.clodmc.command.CommandUsageError;
import au.com.glob.clodmc.command.EitherCommandSender;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Chat;
import au.com.glob.clodmc.util.Schedule;
import au.com.glob.clodmc.util.StringUtil;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Give players the Welcome Book, with rules and customisations */
public class WelcomeBook implements Module, Listener {
  private static final @NotNull String TITLE = "Welcome to Clod-MC";
  private static final @NotNull String AUTHOR = "glob";
  private static final @NotNull List<String> PAGES =
      List.of(
          """
          Welcome to <b>Clod-MC</b>

          Map

            <click:open_url:https://clod.glob.au><blue>clod.glob.au</blue></click>

          Admin

            clod@glob.au

          Discord

            <click:open_url:https://discord.gg/umyFGMZYjU><blue>discord.gg</blue></click>
          """,
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

          A container named in an anvil will display its name when looked at.

          Use wax on an item frame to prevent changes.

          Veinminer enchantment; apply to tool then shift+mine.""");

  public WelcomeBook() {
    CommandBuilder.build(
        "welcome",
        (CommandBuilder builder) -> {
          builder
              .usage("/welcome <player>")
              .description("Give specified player the welcome book")
              .requiresOp();
          builder.executor(
              (@NotNull EitherCommandSender sender, @Nullable Player player) -> {
                if (player == null) {
                  throw new CommandUsageError();
                }
                this.giveWelcomeBook(player, sender);
              });
          builder.completor(
              (@NotNull CommandSender sender, @NotNull List<String> args) ->
                  Bukkit.getOnlinePlayers().stream()
                      .map(Player::getName)
                      .filter(
                          (String name) ->
                              name.toLowerCase().startsWith(args.getFirst().toLowerCase()))
                      .toList());
        });
  }

  @EventHandler
  public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
    if (!event.getPlayer().hasPlayedBefore()) {
      Schedule.delayed(5, () -> WelcomeBook.this.giveWelcomeBook(event.getPlayer(), null));
    }
  }

  private void giveWelcomeBook(@NotNull Player recipient, @Nullable CommandSender sender) {
    ItemStack bookItem = new ItemStack(Material.WRITTEN_BOOK);
    BookMeta bookMeta = (BookMeta) bookItem.getItemMeta();

    BookMeta.BookMetaBuilder builder = bookMeta.toBuilder();
    for (String page : PAGES) {
      builder.addPage(StringUtil.asComponent(page));
    }
    builder.title(Component.text(TITLE)).author(Component.text(AUTHOR));

    bookItem.setItemMeta(builder.build());

    recipient.getInventory().addItem(bookItem);
    if (sender != null) {
      Chat.fyi(sender, "Gave welcome book to " + recipient.getName());
    }
  }
}
