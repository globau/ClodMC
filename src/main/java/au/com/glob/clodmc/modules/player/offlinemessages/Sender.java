package au.com.glob.clodmc.modules.player.offlinemessages;

import au.com.glob.clodmc.util.Chat;
import java.util.Objects;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** wrapper for command sender with chat utilities */
@NullMarked
final class Sender {
  CommandSender recipient;
  String name;

  Sender(final CommandSender recipient, final String name) {
    this.recipient = recipient;
    this.name = name;
  }

  // send an informational message
  void fyi(final String message) {
    Chat.fyi(this.recipient, message);
  }

  // send an error message
  void error(final String message) {
    Chat.error(this.recipient, message);
  }

  @Override
  public boolean equals(@Nullable final Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    final Sender that = (Sender) obj;
    return Objects.equals(this.recipient, that.recipient) && Objects.equals(this.name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.recipient, this.name);
  }
}
