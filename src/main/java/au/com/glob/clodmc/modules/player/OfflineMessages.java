package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Chat;
import au.com.glob.clodmc.util.PlayerDataFile;
import au.com.glob.clodmc.util.PlayerDataUpdater;
import au.com.glob.clodmc.util.Schedule;
import au.com.glob.clodmc.util.StringUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Queue and deliver whispers sent to offline players */
public class OfflineMessages implements Module, Listener {
  @NotNull final Pattern msgPattern = Pattern.compile("^/?msg\\s+(\\S+)\\s+(.+)$");

  public OfflineMessages() {
    ConfigurationSerialization.registerClass(OfflineMessages.Message.class);
  }

  private boolean handleOfflineMsg(
      @NotNull Sender sender, @NotNull String recipient, @NotNull String message) {
    // most messages will be directed at online players, check that first
    Player player = Bukkit.getPlayerExact(recipient);
    if (player != null && player.isOnline()) {
      return false;
    }

    // offline player check might make a web request to fetch uuid
    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(recipient);

    try (PlayerDataUpdater config = PlayerDataUpdater.of(offlinePlayer.getUniqueId())) {
      if (!config.fileExists()) {
        return false;
      }

      List<Message> messages = this.loadMessages(config.getList("messages"));
      if (messages.size() >= 10) {
        sender.error(recipient + "'s mailbox is full");
        return false;
      }
      messages.add(new Message(System.currentTimeMillis() / 1000L, sender.name, message));

      config.set("messages", messages);
    }

    sender.fyi(recipient + " will receive your message next time they log in");
    return true;
  }

  private List<Message> loadMessages(@NotNull Player player) {
    return this.loadMessages(PlayerDataFile.of(player).getList("messages"));
  }

  private List<Message> loadMessages(@Nullable List<?> configValue) {
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

  @EventHandler
  public void onPlayerCommandPreprocess(@NotNull PlayerCommandPreprocessEvent event) {
    Matcher matcher = this.msgPattern.matcher(event.getMessage());
    if (matcher.matches()
        && this.handleOfflineMsg(
            new Sender(event.getPlayer(), event.getPlayer().getName()),
            matcher.group(1),
            matcher.group(2))) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onServerCommand(@NotNull ServerCommandEvent event) {
    Matcher matcher = this.msgPattern.matcher(event.getCommand());
    if (matcher.matches()
        && this.handleOfflineMsg(
            new Sender(Bukkit.getConsoleSender(), "[server]"),
            matcher.group(1),
            matcher.group(2))) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
    Player player = event.getPlayer();
    List<Message> messages = this.loadMessages(player);
    if (messages.isEmpty()) {
      return;
    }

    Schedule.delayed(
        3 * 20,
        () -> {
          for (Message message : messages) {
            message.sendTo(player);
          }
          try (PlayerDataUpdater config = PlayerDataUpdater.of(player)) {
            config.remove("messages");
          }
        });
  }

  private record Sender(@NotNull CommandSender recipient, @NotNull String name) {
    void fyi(@NotNull String message) {
      Chat.fyi(this.recipient, message);
    }

    void error(@NotNull String message) {
      Chat.error(this.recipient, message);
    }
  }

  @SerializableAs("ClodMC.Message")
  public static class Message implements ConfigurationSerializable {
    private final long timestamp;
    private final @NotNull String sender;
    private final @NotNull String message;

    public Message(long timestamp, @NotNull String sender, @NotNull String message) {
      this.timestamp = timestamp;
      this.sender = sender;
      this.message = message;
    }

    public void sendTo(@NotNull Player player) {
      Chat.whisper(
          player,
          StringUtil.relativeTime(System.currentTimeMillis() / 1000L - this.timestamp)
              + " ago "
              + this.sender
              + " whispered to you: "
              + this.message);
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
      Map<String, Object> serialised = new HashMap<>();
      serialised.put("timestamp", this.timestamp);
      serialised.put("sender", this.sender);
      serialised.put("message", this.message);
      return serialised;
    }

    @SuppressWarnings("unused")
    public static @NotNull Message deserialize(@NotNull Map<String, Object> args) {
      return new Message(
          NumberConversions.toLong(args.get("timestamp")),
          (String) args.get("sender"),
          (String) args.get("message"));
    }
  }
}
