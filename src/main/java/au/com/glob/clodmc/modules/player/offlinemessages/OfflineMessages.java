package au.com.glob.clodmc.modules.player.offlinemessages;

import au.com.glob.clodmc.datafile.PlayerDataFile;
import au.com.glob.clodmc.datafile.PlayerDataFiles;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Schedule;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Queue and deliver whispers sent to offline players */
@NullMarked
public class OfflineMessages implements Module, Listener {
  final Pattern msgPattern = Pattern.compile("^/?msg\\s+(\\S+)\\s+(.+)$");

  public OfflineMessages() {
    ConfigurationSerialization.registerClass(Message.class);
  }

  // handle a whisper message intended for an offline player
  private static boolean handleOfflineMsg(Sender sender, String recipient, String message) {
    // most messages will be directed at online players, check that first
    Player player = Bukkit.getPlayerExact(recipient);
    if (player != null && player.isOnline()) {
      return false;
    }

    // offline player check might make a web request to fetch uuid
    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(recipient);

    PlayerDataFile dataFile = PlayerDataFiles.of(offlinePlayer.getUniqueId());
    if (dataFile.isNewFile()) {
      return false;
    }

    List<Message> messages = loadMessages(dataFile.getList("messages"));
    if (messages.size() >= 10) {
      sender.error("%s's mailbox is full".formatted(recipient));
      return false;
    }
    messages.add(new Message(System.currentTimeMillis() / 1000L, sender.name, message));

    dataFile.set("messages", messages);
    dataFile.save();

    sender.fyi("%s will receive your message next time they log in".formatted(recipient));
    return true;
  }

  // load offline messages for a player from their data file
  private static List<Message> loadMessages(Player player) {
    return loadMessages(PlayerDataFiles.of(player).getList("messages"));
  }

  // convert config list to message objects
  private static List<Message> loadMessages(@Nullable List<?> configValue) {
    List<Message> messages = new ArrayList<>();
    if (configValue != null) {
      for (Object obj : configValue) {
        if (obj instanceof Message message) {
          messages.add(message);
        }
      }
    }
    return messages;
  }

  // intercept /msg commands for offline players
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
    Matcher matcher = this.msgPattern.matcher(event.getMessage());
    if (matcher.matches()
        && handleOfflineMsg(
            new Sender(event.getPlayer(), event.getPlayer().getName()),
            matcher.group(1),
            matcher.group(2))) {
      event.setCancelled(true);
    }
  }

  // intercept server /msg commands for offline players
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onServerCommand(ServerCommandEvent event) {
    Matcher matcher = this.msgPattern.matcher(event.getCommand());
    if (matcher.matches()
        && handleOfflineMsg(
            new Sender(Bukkit.getConsoleSender(), "[server]"),
            matcher.group(1),
            matcher.group(2))) {
      event.setCancelled(true);
    }
  }

  // deliver queued messages when player joins
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    List<Message> messages = loadMessages(player);
    if (messages.isEmpty()) {
      return;
    }

    Schedule.delayed(
        3 * 20,
        () -> {
          for (Message message : messages) {
            message.sendTo(player);
          }
          PlayerDataFile dataFile = PlayerDataFiles.of(player);
          dataFile.remove("messages");
          dataFile.save();
        });
  }
}
