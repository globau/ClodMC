package au.com.glob.clodmc.modules.player.welcomebook;

import au.com.glob.clodmc.annotations.Audience;
import au.com.glob.clodmc.annotations.Doc;
import au.com.glob.clodmc.command.CommandBuilder;
import au.com.glob.clodmc.command.CommandUsageError;
import au.com.glob.clodmc.command.EitherCommandSender;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Chat;
import au.com.glob.clodmc.util.Schedule;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@Doc(
    audience = Audience.ADMIN,
    title = "Welcome Book",
    description = "Give players the Welcome Book")
@NullMarked
public class WelcomeBookAdmin extends Module implements Listener {
  public WelcomeBookAdmin() {
    CommandBuilder.build("welcome-player")
        .usage("/welcome-player <player>")
        .description("Give specified player the welcome book")
        .requiresOp()
        .executor(
            (final EitherCommandSender sender, @Nullable final Player player) -> {
              if (player == null) {
                throw new CommandUsageError();
              }
              WelcomeBook.giveTo(player);
              Chat.fyi(sender, "Gave welcome book to %s".formatted(player.getName()));
            })
        .completor(
            (CommandSender sender, List<String> args) ->
                Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(
                        (String name) ->
                            name.toLowerCase(Locale.ENGLISH)
                                .startsWith(args.getFirst().toLowerCase(Locale.ENGLISH)))
                    .toList());

    WelcomeBook.exportAsJson();
  }

  // give welcome book to new players
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerJoin(final PlayerJoinEvent event) {
    if (!event.getPlayer().hasPlayedBefore()) {
      Schedule.delayed(5, () -> WelcomeBook.giveTo(event.getPlayer()));
    }
  }
}
