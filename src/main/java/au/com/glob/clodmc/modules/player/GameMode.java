package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.command.CommandBuilder;
import au.com.glob.clodmc.command.CommandError;
import au.com.glob.clodmc.command.CommandUsageError;
import au.com.glob.clodmc.command.EitherCommandSender;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Chat;
import au.com.glob.clodmc.util.StringUtil;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** /gamemode that doesn't announce */
@NullMarked
public class GameMode implements Module {
  private static final List<String> MODES = List.of("survival", "creative", "spectator");

  public GameMode() {
    CommandBuilder.build("gamemode")
        .usage("/gamemode <survival|creative|spectator> [player]")
        .description("Change player's Game Mode")
        .requiresOp()
        .executor(
            (EitherCommandSender sender, @Nullable String mode, @Nullable Player target) -> {
              if (sender.isPlayer() && !sender.isOp()) {
                throw new CommandError("You cannot change your game mode");
              }

              if (mode == null) {
                throw new CommandUsageError();
              }
              org.bukkit.GameMode gameMode =
                  switch (mode) {
                    case "survival" -> org.bukkit.GameMode.SURVIVAL;
                    case "creative" -> org.bukkit.GameMode.CREATIVE;
                    case "spectator" -> org.bukkit.GameMode.SPECTATOR;
                    default -> throw new CommandError("Invalid game mode");
                  };

              if (target == null) {
                if (sender.isPlayer()) {
                  target = sender.asPlayer();
                } else {
                  throw new CommandError("Target player name required");
                }
              }

              target.setGameMode(gameMode);
              mode = StringUtil.toTitleCase(mode);

              if (sender.is(target)) {
                Chat.fyi(sender, "Set own game mode to " + mode);
              } else {
                Chat.fyi(sender, "Set " + target.getName() + "'s game mode to " + mode);
                Chat.info(target, "Your game mode has been changed to " + mode);
              }
            })
        .completor(
            (CommandSender sender, List<String> args) -> {
              // no args
              if (args.isEmpty()) {
                return MODES;
              }
              // game mode
              if (args.size() == 1) {
                return MODES.stream()
                    .filter((String value) -> value.startsWith(args.getFirst()))
                    .toList();
              }
              // player name
              return Bukkit.getOnlinePlayers().stream()
                  .map(Player::getName)
                  .filter((String name) -> name.toLowerCase(Locale.ENGLISH).startsWith(args.get(1)))
                  .toList();
            });
  }
}
