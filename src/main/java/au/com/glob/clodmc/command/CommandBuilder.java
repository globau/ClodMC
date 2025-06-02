package au.com.glob.clodmc.command;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.util.Chat;
import au.com.glob.clodmc.util.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"UnusedReturnValue", "SameParameterValue"})
public class CommandBuilder {
  private @Nullable String name;
  private @Nullable String usage;
  private @Nullable String description;
  private @Nullable Executor executor;
  private @Nullable Completor completor;
  private boolean requiresOp = false;

  public static void build(@NotNull String name, @NotNull Consumer<CommandBuilder> handler) {
    CommandBuilder builder = new CommandBuilder().name(name);
    handler.accept(builder);
    builder.register();
  }

  public @NotNull CommandBuilder name(@NotNull String name) {
    this.name = name;
    return this;
  }

  public @NotNull CommandBuilder usage(@NotNull String usage) {
    this.usage = usage;
    return this;
  }

  public @NotNull CommandBuilder description(@NotNull String description) {
    this.description = description;
    return this;
  }

  public @NotNull CommandBuilder requiresOp() {
    this.requiresOp = true;
    return this;
  }

  public @NotNull CommandBuilder executor(@NotNull ExecutorP executor) {
    this.executor = executor;
    return this;
  }

  public @NotNull CommandBuilder executor(@NotNull ExecutorPS executor) {
    this.executor = executor;
    return this;
  }

  public @NotNull CommandBuilder executor(@NotNull ExecutorES executor) {
    this.executor = executor;
    return this;
  }

  public @NotNull CommandBuilder executor(@NotNull ExecutorESS executor) {
    this.executor = executor;
    return this;
  }

  public @NotNull CommandBuilder executor(@NotNull ExecutorEP executor) {
    this.executor = executor;
    return this;
  }

  public @NotNull CommandBuilder executor(@NotNull ExecutorEPS executor) {
    this.executor = executor;
    return this;
  }

  public @NotNull CommandBuilder executor(@NotNull ExecutorESP executor) {
    this.executor = executor;
    return this;
  }

  public @NotNull CommandBuilder executor(@NotNull ExecutorE executor) {
    this.executor = executor;
    return this;
  }

  public @NotNull CommandBuilder completor(@NotNull CompletorP completor) {
    this.completor = completor;
    return this;
  }

  public @NotNull CommandBuilder completor(@NotNull CompletorS completor) {
    this.completor = completor;
    return this;
  }

  private void register() {
    if (this.name == null || this.description == null || this.executor == null) {
      throw new RuntimeException("incomplete command");
    }

    if (this.usage == null) {
      this.usage = "/" + this.name;
    }

    Command command =
        new Command(this.name, this.description, this.usage, List.of()) {
          @Override
          public boolean execute(
              @NotNull CommandSender sender,
              @NotNull String commandLabel,
              String @NotNull [] args) {
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
          public @NotNull List<String> tabComplete(
              @NotNull CommandSender sender, @NotNull String alias, String @NotNull [] args)
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

  private @NotNull Player toPlayer(@NotNull CommandSender sender) throws CommandError {
    if (!(sender instanceof Player player)) {
      throw new CommandError("This command can only be run by a player");
    }
    if (this.requiresOp && !player.isOp()) {
      throw new CommandError("You do not have permissions to run this command");
    }
    return player;
  }

  private @Nullable Player toPlayer(String @NotNull [] args, int index) {
    if (args.length - 1 < index || args[index] == null || args[index].isEmpty()) {
      return null;
    }
    Player player = Bukkit.getPlayerExact(args[index]);
    if (player == null) {
      throw new CommandError("Unknown player: " + args[index]);
    }
    return player;
  }

  private @Nullable String toString(String @NotNull [] args, int index) {
    if (args.length - 1 < index || args[index] == null || args[index].isEmpty()) {
      return null;
    }
    return args[index].isEmpty() ? null : args[index];
  }
}
