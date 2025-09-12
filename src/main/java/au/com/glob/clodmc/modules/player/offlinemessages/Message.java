package au.com.glob.clodmc.modules.player.offlinemessages;

import au.com.glob.clodmc.util.Chat;
import au.com.glob.clodmc.util.StringUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.bukkit.util.NumberConversions;
import org.jspecify.annotations.NullMarked;

/** represents a whisper message sent to an offline player */
@SerializableAs("ClodMC.Message")
@NullMarked
public class Message implements ConfigurationSerializable {
  private final long timestamp;
  private final String sender;
  private final String message;

  public Message(final long timestamp, final String sender, final String message) {
    this.timestamp = timestamp;
    this.sender = sender;
    this.message = message;
  }

  // send this message to a player with timestamp
  public void sendTo(final Player player) {
    Chat.whisper(
        player,
        "%s ago %s whispered to you: %s"
            .formatted(
                StringUtil.relativeTime(System.currentTimeMillis() / 1000L - this.timestamp),
                this.sender,
                this.message));
  }

  // serialise message for datafile
  @Override
  public Map<String, Object> serialize() {
    final Map<String, Object> serialised = new HashMap<>();
    serialised.put("timestamp", this.timestamp);
    serialised.put("sender", this.sender);
    serialised.put("message", this.message);
    return serialised;
  }

  // deserialise message from datafile
  @SuppressWarnings("unused")
  public static Message deserialize(final Map<String, Object> args) {
    return new Message(
        NumberConversions.toLong(args.get("timestamp")),
        Objects.requireNonNull((String) args.get("sender")),
        Objects.requireNonNull((String) args.get("message")));
  }
}
