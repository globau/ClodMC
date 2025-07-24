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

@SerializableAs("ClodMC.Message")
@NullMarked
public class Message implements ConfigurationSerializable {
  private final long timestamp;
  private final String sender;
  private final String message;

  public Message(long timestamp, String sender, String message) {
    this.timestamp = timestamp;
    this.sender = sender;
    this.message = message;
  }

  public void sendTo(Player player) {
    Chat.whisper(
        player,
        StringUtil.relativeTime(System.currentTimeMillis() / 1000L - this.timestamp)
            + " ago "
            + this.sender
            + " whispered to you: "
            + this.message);
  }

  @Override
  public Map<String, Object> serialize() {
    Map<String, Object> serialised = new HashMap<>();
    serialised.put("timestamp", this.timestamp);
    serialised.put("sender", this.sender);
    serialised.put("message", this.message);
    return serialised;
  }

  @SuppressWarnings("unused")
  public static Message deserialize(Map<String, Object> args) {
    return new Message(
        NumberConversions.toLong(args.get("timestamp")),
        Objects.requireNonNull((String) args.get("sender")),
        Objects.requireNonNull((String) args.get("message")));
  }
}
