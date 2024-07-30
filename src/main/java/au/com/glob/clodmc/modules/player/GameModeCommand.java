package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.modules.CommandError;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.SimpleCommand;
import au.com.glob.clodmc.util.Chat;
import au.com.glob.clodmc.util.StringUtil;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GameModeCommand extends SimpleCommand implements Module {
  private static final @NotNull List<String> MODES = List.of("survival", "creative", "spectator");

  public GameModeCommand() {
    super(
        "gamemode",
        "/gamemode <survival|creative|spectator> [player]",
        "Change player's Game Mode");
  }

  @Override
  protected void execute(@NotNull CommandSender sender, @NotNull List<String> args) {
    if (sender instanceof Player player && !player.isOp()) {
      throw new CommandError("You cannot change your game mode");
    }

    String mode = this.popArg(args);
    GameMode gameMode =
        switch (mode) {
          case "survival" -> GameMode.SURVIVAL;
          case "creative" -> GameMode.CREATIVE;
          case "spectator" -> GameMode.SPECTATOR;
          default -> throw new CommandError(this.usageMessage);
        };

    String name = this.popArg(args, "");
    Player target = null;
    if (name.isEmpty()) {
      if (sender instanceof Player player) {
        target = player;
      } else {
        throw new CommandError("Target player name required");
      }
    }
    if (target == null) {
      target = Bukkit.getPlayerExact(name);
      if (target == null) {
        throw new CommandError("Unknown player: " + name);
      }
    }

    target.setGameMode(gameMode);
    mode = StringUtil.toTitleCase(mode);

    if (sender.equals(target)) {
      Chat.fyi(sender, "Set own game mode to " + mode);
    } else {
      Chat.fyi(sender, "Set " + target.getName() + "'s game mode to " + mode);
      Chat.info(target, "Your game mode has been changed to " + mode);
    }
  }

  @Override
  public @NotNull List<String> tabComplete(
      @NotNull CommandSender sender, @NotNull String alias, String @NotNull [] args)
      throws IllegalArgumentException {
    if (args.length == 0) {
      return MODES;
    } else if (args.length == 1) {
      return MODES.stream().filter((String value) -> value.startsWith(args[0])).toList();
    } else {
      String arg = args[1].toLowerCase();
      return Bukkit.getOnlinePlayers().stream()
          .map(Player::getName)
          .filter((String name) -> name.toLowerCase().startsWith(arg))
          .toList();
    }
  }
}
