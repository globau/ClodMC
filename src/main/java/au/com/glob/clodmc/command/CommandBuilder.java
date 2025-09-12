package au.com.glob.clodmc.command;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.util.Chat;
import au.com.glob.clodmc.util.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Builder for creating type-safe Bukkit commands with automatic argument parsing.
 *
 * <p>The command framework uses a type-safe executor pattern where different executor interfaces
 * correspond to different command signatures. Interface names use letter codes:
 *
 * <ul>
 *   <li>E - either player or console sender
 *   <li>P - player argument
 *   <li>S - string argument
 * </ul>
 *
 * <p>Executor types combine these codes to define command signatures (e.g., ExecutorEPS = either
 * sender + player argument + string argument).
 *
 * <p>Arguments are automatically parsed and validated. Player arguments resolve player names to
 * Player objects, with automatic error handling for unknown players. String arguments are extracted
 * from command arguments and can be null if not provided.
 *
 * <p>All commands are automatically registered with error handling, usage message display, and
 * permission checking.
 */
@NullMarked
@SuppressWarnings({"UnusedReturnValue", "SameParameterValue"})
public final class CommandBuilder {
  private final String name;
  private @Nullable String usage;
  private @Nullable String description;
  private @Nullable Executor executor;
  private @Nullable Completor completor;
  private boolean requiresOp = false;

  private static final List<CommandBuilder> builders = new ArrayList<>();

  private CommandBuilder(final String name) {
    this.name = name;
  }

  // create new command builder with given name
  public static CommandBuilder build(final String name) {
    final CommandBuilder builder = new CommandBuilder(name);
    builders.add(builder);
    return builder;
  }

  // register all built commands with bukkit
  public static void registerBuilders() {
    for (final CommandBuilder builder : builders) {
      builder.register();
    }
    builders.clear();
  }

  // set command usage string
  public CommandBuilder usage(final String usage) {
    this.usage = usage;
    return this;
  }

  // set command description
  public CommandBuilder description(final String description) {
    this.description = description;
    return this;
  }

  // require op permissions to run this command
  public CommandBuilder requiresOp() {
    this.requiresOp = true;
    return this;
  }

  // set executor for player-only commands
  public CommandBuilder executor(final ExecutorP executor) {
    this.executor = executor;
    return this;
  }

  // set executor for player commands with string argument
  public CommandBuilder executor(final ExecutorPS executor) {
    this.executor = executor;
    return this;
  }

  // set executor for commands accepting any sender with string argument
  public CommandBuilder executor(final ExecutorES executor) {
    this.executor = executor;
    return this;
  }

  // set executor for commands accepting any sender with two string arguments
  public CommandBuilder executor(final ExecutorESS executor) {
    this.executor = executor;
    return this;
  }

  // set executor for commands accepting any sender with player argument
  public CommandBuilder executor(final ExecutorEP executor) {
    this.executor = executor;
    return this;
  }

  // set executor for commands accepting any sender with player and string arguments
  public CommandBuilder executor(final ExecutorEPS executor) {
    this.executor = executor;
    return this;
  }

  // set executor for commands accepting any sender with string and player arguments
  public CommandBuilder executor(final ExecutorESP executor) {
    this.executor = executor;
    return this;
  }

  // set executor for commands accepting any sender with no arguments
  public CommandBuilder executor(final ExecutorE executor) {
    this.executor = executor;
    return this;
  }

  // set tab completion handler for player commands
  public CommandBuilder completor(final CompletorP completor) {
    this.completor = completor;
    return this;
  }

  // set tab completion handler for commands with sender
  public CommandBuilder completor(final CompletorS completor) {
    this.completor = completor;
    return this;
  }

  // register command with bukkit command map
  private void register() {
    if (this.description == null || this.executor == null) {
      throw new RuntimeException("incomplete command: %s".formatted(this.name));
    }

    if (this.usage == null) {
      this.usage = "/%s".formatted(this.name);
    }

    final Command command =
        new Command(this.name, this.description, this.usage, List.of()) {
          @Override
          public boolean execute(
              final CommandSender sender, final String commandLabel, final String[] args) {
            final CommandBuilder that = CommandBuilder.this;
            if (that.executor == null) {
              return false;
            }
            try {
              switch (that.executor) {
                case final ExecutorP executorP -> executorP.accept(that.toPlayer(sender));
                case final ExecutorE executorE -> executorE.accept(new EitherCommandSender(sender));
                case final ExecutorEP executorEP ->
                    executorEP.accept(new EitherCommandSender(sender), argToPlayer(args, 0));
                case final ExecutorEPS executorEPS ->
                    executorEPS.accept(
                        new EitherCommandSender(sender),
                        argToPlayer(args, 0),
                        argToString(args, 1));
                case final ExecutorES executorES ->
                    executorES.accept(new EitherCommandSender(sender), argToString(args, 0));
                case final ExecutorPS executorPS ->
                    executorPS.accept(that.toPlayer(sender), argToString(args, 0));
                case final ExecutorESS executorESS ->
                    executorESS.accept(
                        new EitherCommandSender(sender),
                        argToString(args, 0),
                        argToString(args, 1));
                case final ExecutorESP executorESP ->
                    executorESP.accept(
                        new EitherCommandSender(sender),
                        argToString(args, 0),
                        argToPlayer(args, 1));
                default -> throw new RuntimeException("executor not handled");
              }
            } catch (final CommandUsageError e) {
              Chat.error(sender, "usage: %s".formatted(this.usageMessage));
            } catch (final CommandError e) {
              Chat.error(sender, Objects.requireNonNullElse(e.getMessage(), "Internal Error"));
            } catch (final Throwable e) {
              final String message =
                  e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
              Chat.error(sender, "Internal command error: %s".formatted(message));
              Logger.exception(e);
            }
            return true;
          }

          @Override
          public List<String> tabComplete(
              final CommandSender sender, final String alias, final String[] args)
              throws IllegalArgumentException {
            final CommandBuilder that = CommandBuilder.this;
            if (that.completor == null) {
              return List.of();
            }

            final List<String> argsList = new ArrayList<>(List.of(args));
            return switch (that.completor) {
              case final CompletorP completorP ->
                  completorP.accept(that.toPlayer(sender), argsList);
              case final CompletorS completorS -> completorS.accept(sender, argsList);
              default -> throw new RuntimeException("completor not handled");
            };
          }
        };
    if (this.requiresOp) {
      command.setPermission("op");
    }

    ClodMC.instance.getServer().getCommandMap().register("clod-mc", command);
  }

  // convert command sender to player with permission checks
  private Player toPlayer(final CommandSender sender) throws CommandError {
    if (!(sender instanceof final Player player)) {
      throw new CommandError("This command can only be run by a player");
    }
    if (this.requiresOp && !player.isOp()) {
      throw new CommandError("You do not have permissions to run this command");
    }
    return player;
  }

  // parse player argument from command args
  private static @Nullable Player argToPlayer(final String[] args, final int index) {
    if (args.length - 1 < index || args[index].isEmpty()) {
      return null;
    }
    final Player player = Bukkit.getPlayerExact(args[index]);
    if (player == null) {
      throw new CommandError("Unknown player: %s".formatted(args[index]));
    }
    return player;
  }

  // parse string argument from command args
  private static @Nullable String argToString(final String[] args, final int index) {
    if (args.length - 1 < index || args[index].isEmpty()) {
      return null;
    }
    return args[index];
  }
}
