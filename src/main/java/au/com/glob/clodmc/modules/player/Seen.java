package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.command.CommandBuilder;
import au.com.glob.clodmc.command.CommandUsageError;
import au.com.glob.clodmc.command.EitherCommandSender;
import au.com.glob.clodmc.datafile.PlayerDataFile;
import au.com.glob.clodmc.datafile.PlayerDataFiles;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Chat;
import au.com.glob.clodmc.util.Players;
import au.com.glob.clodmc.util.StringUtil;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** /seen command; show how long it's been since the server last saw the player */
public class Seen implements Module, Listener {
  public Seen() {
    CommandBuilder.build("seen")
        .usage("/seen <player>")
        .description("Show time since player's last login")
        .executor(
            (@NotNull EitherCommandSender sender, @Nullable String playerName) -> {
              if (playerName == null) {
                throw new CommandUsageError();
              }

              if (sender.isPlayer() && playerName.equalsIgnoreCase(sender.asPlayer().getName())) {
                Chat.info(sender, "You're online now");
                return;
              }

              Player player = Bukkit.getPlayerExact(playerName);
              if (player != null) {
                Chat.info(sender, playerName + " is online now");
                return;
              }

              UUID playerUUID = Players.getWhitelistedUUID(playerName);
              if (playerUUID == null) {
                Chat.error(sender, playerName + " doesn't play on this server");
                return;
              }
              PlayerDataFile dataFile = PlayerDataFiles.of(playerUUID);

              LocalDateTime date = dataFile.getLastLogout();
              if (date == null) {
                date = dataFile.getLastLogin();
              }
              if (date == null) {
                Chat.warning(sender, "Not sure when " + playerName + " last played");
                return;
              }

              String dateAgo =
                  StringUtil.relativeTime(
                      System.currentTimeMillis() / 1000L - date.toEpochSecond(ZoneOffset.of("+8")));
              Chat.info(sender, playerName + " was last seen " + dateAgo + " ago");
            })
        .completor(
            (@NotNull CommandSender sender, @NotNull List<String> args) ->
                Players.getWhitelisted().keySet().stream()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .filter(
                        (String name) ->
                            name.toLowerCase(Locale.ENGLISH)
                                .startsWith(args.getFirst().toLowerCase(Locale.ENGLISH)))
                    .toList());
  }
}
