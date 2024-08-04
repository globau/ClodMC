package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.command.CommandBuilder;
import au.com.glob.clodmc.command.CommandUsageError;
import au.com.glob.clodmc.command.EitherCommandSender;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Chat;
import au.com.glob.clodmc.util.PlayerDataFile;
import au.com.glob.clodmc.util.StringUtil;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** /seen command; show how long it's been since the server last saw the player */
public class Seen implements Module, Listener {
  private final @NotNull Map<String, UUID> validNames = new HashMap<>();

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

              PlayerDataFile config =
                  this.validNames.entrySet().stream()
                      .filter(
                          (Map.Entry<String, UUID> entry) ->
                              entry.getKey().equalsIgnoreCase(playerName))
                      .map((Map.Entry<String, UUID> entry) -> PlayerDataFile.of(entry.getValue()))
                      .findFirst()
                      .orElse(null);

              if (config == null || !config.fileExists()) {
                Chat.error(sender, playerName + " doesn't play on this server");
                return;
              }

              LocalDateTime date = config.getLastLogout();
              if (date == null) {
                date = config.getLastLogin();
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
                this.validNames.keySet().stream()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .filter(
                        (String name) ->
                            name.toLowerCase().startsWith(args.getFirst().toLowerCase()))
                    .toList())
        .register();
  }

  private void updateValidNames() {
    this.validNames.clear();
    for (UUID uuid : PlayerDataFile.knownUUIDs()) {
      PlayerDataFile config = PlayerDataFile.of(uuid);
      if (config.getInt("player.playtime_min", 0) > 10) {
        this.validNames.put(config.getPlayerName(), uuid);
      }
    }
  }

  @EventHandler
  public void onServerLoad(@NotNull ServerLoadEvent event) {
    this.updateValidNames();
  }

  @EventHandler
  public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
    this.updateValidNames();
  }

  @EventHandler
  public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
    this.updateValidNames();
  }
}
