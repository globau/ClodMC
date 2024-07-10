package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.SimpleCommand;
import au.com.glob.clodmc.util.MiscUtil;
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
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public class SeenCommand extends SimpleCommand implements Module, Listener {
  private final @NotNull Map<String, UUID> validNames = new HashMap<>();

  public SeenCommand() {
    super("seen", "/seen <player>", "Show time since player's last login");
    this.updateValidNames();
  }

  private void updateValidNames() {
    this.validNames.clear();
    for (UUID uuid : PlayerData.seenUUIDs()) {
      PlayerData.Config config = PlayerData.of(uuid);
      if (config.getInt("player.playtime_min", 0) > 10) {
        this.validNames.put(config.getPlayerName(), uuid);
      }
    }
  }

  @EventHandler
  public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
    this.updateValidNames();
  }

  @Override
  protected void execute(@NotNull CommandSender sender, @NotNull List<String> args) {
    String playerName = this.popArg(args);

    if (sender instanceof Player player && playerName.equalsIgnoreCase(player.getName())) {
      sender.sendRichMessage("<yellow>you're online now");
      return;
    }

    Player player = Bukkit.getPlayerExact(playerName);
    if (player != null) {
      sender.sendRichMessage("<yellow>" + playerName + " is online now");
      return;
    }

    PlayerData.Config config =
        this.validNames.entrySet().stream()
            .filter((Map.Entry<String, UUID> entry) -> entry.getKey().equalsIgnoreCase(playerName))
            .map((Map.Entry<String, UUID> entry) -> PlayerData.of(entry.getValue()))
            .findFirst()
            .orElse(null);

    if (config == null || !config.exists) {
      sender.sendRichMessage("<yellow>" + playerName + " doesn't play on this server");
      return;
    }

    LocalDateTime date = config.getLastLogout();
    if (date == null) {
      date = config.getLastLogin();
    }
    if (date == null) {
      sender.sendRichMessage("<yellow>not sure when " + playerName + " last played");
      return;
    }

    String dateAgo =
        MiscUtil.relativeTime(
            System.currentTimeMillis() / 1000L - date.toEpochSecond(ZoneOffset.of("+8")));
    sender.sendRichMessage("<yellow>" + playerName + " was last seen " + dateAgo + " ago");
  }

  @Override
  public @NotNull List<String> tabComplete(
      @NotNull CommandSender sender, @NotNull String alias, @NotNull String @NotNull [] args)
      throws IllegalArgumentException {
    return this.validNames.keySet().stream()
        .sorted(String.CASE_INSENSITIVE_ORDER)
        .filter((String name) -> name.toLowerCase().startsWith(args[0].toLowerCase()))
        .toList();
  }
}
