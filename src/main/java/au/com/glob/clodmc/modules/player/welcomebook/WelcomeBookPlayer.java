package au.com.glob.clodmc.modules.player.welcomebook;

import au.com.glob.clodmc.annotations.Audience;
import au.com.glob.clodmc.annotations.Doc;
import au.com.glob.clodmc.command.CommandBuilder;
import au.com.glob.clodmc.command.CommandError;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Chat;
import au.com.glob.clodmc.util.Schedule;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@Doc(
    audience = Audience.PLAYER,
    title = "Welcome Book",
    description = "Give yourself the Welcome Book")
@NullMarked
public class WelcomeBookPlayer implements Module {
  private final Set<UUID> cooldownUUIDs = new HashSet<>();

  public WelcomeBookPlayer() {
    CommandBuilder.build("welcome-book")
        .description("Give yourself the welcome book")
        .executor(
            (final Player player) -> {
              if (this.cooldownUUIDs.contains(player.getUniqueId())) {
                throw new CommandError("You need to wait longer before requesting another book");
              }
              this.cooldownUUIDs.add(player.getUniqueId());
              Schedule.delayed(20 * 60, () -> this.cooldownUUIDs.remove(player.getUniqueId()));
              WelcomeBook.giveTo(player);
              Chat.fyi(player, "Enjoy the welcome book");
            });
  }
}
