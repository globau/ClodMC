package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.MiscUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;

public class OfflineMessages implements Module, Listener {
  Pattern msgPattern = Pattern.compile("^/?msg\\s+(\\S+)\\s+(.+)$");

  public OfflineMessages() {
    ConfigurationSerialization.registerClass(OfflineMessages.Message.class);
  }

  private boolean handleOfflineMsg(Sender sender, String recipient, String message) {
    // most messages will be directed at online players, check that first
    Player player = Bukkit.getPlayerExact(recipient);
    if (player != null && player.isOnline()) {
      return false;
    }

    // offline player check might make a web request to fetch uuid
    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(recipient);

    try (PlayerData.Update config = new PlayerData.Update(offlinePlayer.getUniqueId())) {
      if (!config.exists()) {
        return false;
      }

      List<Message> messages = this.loadMessages(config.getList("messages"));
      if (messages.size() >= 10) {
        sender.sendRichMessage("<red>" + recipient + "'s mailbox is full");
        return false;
      }
      messages.add(new Message(System.currentTimeMillis() / 1000L, sender.getName(), message));

      config.set("messages", messages);
    }

    sender.sendRichMessage("<yellow><i>message queued for delivery");
    return true;
  }

  private List<Message> loadMessages(Player player) {
    return this.loadMessages(PlayerData.of(player).getList("messages"));
  }

  private List<Message> loadMessages(List<?> configValue) {
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
  public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
    Matcher matcher = this.msgPattern.matcher(event.getMessage());
    if (matcher.matches()
        && this.handleOfflineMsg(
            new PlayerSender(event.getPlayer()), matcher.group(1), matcher.group(2))) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onServerCommand(ServerCommandEvent event) {
    Matcher matcher = this.msgPattern.matcher(event.getCommand());
    if (matcher.matches()
        && this.handleOfflineMsg(new ConsoleSender(), matcher.group(1), matcher.group(2))) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    List<Message> messages = this.loadMessages(player);
    if (messages.isEmpty()) {
      return;
    }

    Bukkit.getScheduler()
        .runTaskLater(
            ClodMC.instance,
            () -> {
              for (Message message : messages) {
                message.sendTo(player);
              }
              try (PlayerData.Update config = new PlayerData.Update(player)) {
                config.set("messages", null);
              }
            },
            20 * 3);
  }

  private interface Sender {
    @NotNull String getName();

    void sendRichMessage(@NotNull String message);
  }

  private record PlayerSender(Player player) implements Sender {
    private PlayerSender(@NotNull Player player) {
      this.player = player;
    }

    public @NotNull String getName() {
      return this.player.getName();
    }

    public void sendRichMessage(@NotNull String message) {
      this.player.sendRichMessage(message);
    }
  }

  private static class ConsoleSender implements Sender {
    public @NotNull String getName() {
      return "[server]";
    }

    public void sendRichMessage(@NotNull String message) {
      Bukkit.getConsoleSender().sendRichMessage(message);
    }
  }

  public static class Message implements ConfigurationSerializable {
    private final long timestamp;
    private final String sender;
    private final String message;

    public Message(long timestamp, String sender, String message) {
      this.timestamp = timestamp;
      this.sender = sender;
      this.message = message;
    }

    public void sendTo(@NotNull Player player) {
      player.sendRichMessage(
          "<grey><i>"
              + MiscUtil.relativeTime(System.currentTimeMillis() / 1000L - this.timestamp)
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
