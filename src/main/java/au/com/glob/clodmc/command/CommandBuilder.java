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
public class CommandBuilder {
  private final String name;
  private @Nullable String usage;
  private @Nullable String description;
  private @Nullable Executor executor;
  private @Nullable Completor completor;
  private boolean requiresOp = false;

  private static final List<CommandBuilder> builders = new ArrayList<>();

  private CommandBuilder(String name) {
    this.name = name;
  }

  public static CommandBuilder build(String name) {
    CommandBuilder builder = new CommandBuilder(name);
    builders.add(builder);
    return builder;
  }

  public static void registerBuilders() {
    for (CommandBuilder builder : builders) {
      builder.register();
    }
    builders.clear();
  }

  public CommandBuilder usage(String usage) {
    this.usage = usage;
    return this;
  }

  public CommandBuilder description(String description) {
    this.description = description;
    return this;
  }

  public CommandBuilder requiresOp() {
    this.requiresOp = true;
    return this;
  }

  public CommandBuilder executor(ExecutorP executor) {
    this.executor = executor;
    return this;
  }

  public CommandBuilder executor(ExecutorPS executor) {
    this.executor = executor;
    return this;
  }

  public CommandBuilder executor(ExecutorES executor) {
    this.executor = executor;
    return this;
  }

  public CommandBuilder executor(ExecutorESS executor) {
    this.executor = executor;
    return this;
  }

  public CommandBuilder executor(ExecutorEP executor) {
    this.executor = executor;
    return this;
  }

  public CommandBuilder executor(ExecutorEPS executor) {
    this.executor = executor;
    return this;
  }

  public CommandBuilder executor(ExecutorESP executor) {
    this.executor = executor;
    return this;
  }

  public CommandBuilder executor(ExecutorE executor) {
    this.executor = executor;
    return this;
  }

  public CommandBuilder completor(CompletorP completor) {
    this.completor = completor;
    return this;
  }

  public CommandBuilder completor(CompletorS completor) {
    this.completor = completor;
    return this;
  }

  private void register() {
    if (this.description == null || this.executor == null) {
      throw new RuntimeException("incomplete command: " + this.name);
    }

    if (this.usage == null) {
      this.usage = "/" + this.name;
    }

    Command command =
        new Command(this.name, this.description, this.usage, List.of()) {
          @Override
          public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            CommandBuilder that = CommandBuilder.this;
            if (that.executor == null) {
              return false;
            }
            try {
              switch (that.executor) {
                case ExecutorP executorP -> executorP.accept(that.toPlayer(sender));
                case ExecutorE executorE -> executorE.accept(new EitherCommandSender(sender));
                case ExecutorEP executorEP ->
                    executorEP.accept(new EitherCommandSender(sender), that.toPlayer(args, 0));
                case ExecutorEPS executorEPS ->
                    executorEPS.accept(
                        new EitherCommandSender(sender),
                        that.toPlayer(args, 0),
                        that.toString(args, 1));
                case ExecutorES executorES ->
                    executorES.accept(new EitherCommandSender(sender), that.toString(args, 0));
                case ExecutorPS executorPS ->
                    executorPS.accept(that.toPlayer(sender), that.toString(args, 0));
                case ExecutorESS executorESS ->
                    executorESS.accept(
                        new EitherCommandSender(sender),
                        that.toString(args, 0),
                        that.toString(args, 1));
                case ExecutorESP executorESP ->
                    executorESP.accept(
                        new EitherCommandSender(sender),
                        that.toString(args, 0),
                        that.toPlayer(args, 1));
                default -> throw new RuntimeException("executor not handled");
              }
            } catch (CommandUsageError e) {
              Chat.error(sender, "usage: " + this.usageMessage);
            } catch (CommandError e) {
              Chat.error(sender, Objects.requireNonNullElse(e.getMessage(), "Internal Error"));
            } catch (Throwable e) {
              String message =
                  e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
              Chat.error(sender, "Internal command error: " + message);
              Logger.exception(e);
            }
            return true;
          }

          @Override
          public List<String> tabComplete(CommandSender sender, String alias, String[] args)
              throws IllegalArgumentException {
            CommandBuilder that = CommandBuilder.this;
            if (that.completor == null) {
              return List.of();
            }

            List<String> argsList = new ArrayList<>(List.of(args));
            return switch (that.completor) {
              case CompletorP completorP -> completorP.accept(that.toPlayer(sender), argsList);
              case CompletorS completorS -> completorS.accept(sender, argsList);
              default -> throw new RuntimeException("completor not handled");
            };
          }
        };
    if (this.requiresOp) {
      command.setPermission("op");
    }

    ClodMC.instance.getServer().getCommandMap().register("clod-mc", command);
  }

  private Player toPlayer(CommandSender sender) throws CommandError {
    if (!(sender instanceof Player player)) {
      throw new CommandError("This command can only be run by a player");
    }
    if (this.requiresOp && !player.isOp()) {
      throw new CommandError("You do not have permissions to run this command");
    }
    return player;
  }

  private @Nullable Player toPlayer(String[] args, int index) {
    if (args.length - 1 < index || args[index].isEmpty()) {
      return null;
    }
    Player player = Bukkit.getPlayerExact(args[index]);
    if (player == null) {
      throw new CommandError("Unknown player: " + args[index]);
    }
    return player;
  }

  private @Nullable String toString(String[] args, int index) {
    if (args.length - 1 < index || args[index].isEmpty()) {
      return null;
    }
    return args[index];
  }
}
