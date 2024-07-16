package au.com.glob.clodmc.modules;

import au.com.glob.clodmc.ClodMC;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public abstract class SimpleCommand extends Command {
  private static final @NotNull List<String> commandNames = new ArrayList<>();

  protected SimpleCommand(@NotNull String name, @NotNull String description) {
    this(name, description, "/" + name);
  }

  protected SimpleCommand(
      @NotNull String name, @NotNull String usage, @NotNull String description) {
    super(name, description, usage, List.of());
    this.setDescription(description);
    ClodMC.instance.getServer().getCommandMap().register("clod-mc", this);
    commandNames.add(this.getName());
  }

  public static void unregisterAll() {
    CommandMap commandMap = ClodMC.instance.getServer().getCommandMap();
    for (String name : commandNames) {
      Command command = commandMap.getCommand(name);
      if (command != null) {
        command.unregister(commandMap);
      }
    }
    commandNames.clear();
  }

  // execution

  protected abstract void execute(@NotNull CommandSender sender, @NotNull List<String> args);

  @Override
  public boolean execute(
      @NotNull CommandSender sender,
      @NotNull String commandLabel,
      @NotNull String @NotNull [] args) {
    try {
      this.execute(sender, new ArrayList<>(List.of(args)));
    } catch (CommandError e) {
      ClodMC.error(sender, e.getMessage());
    } catch (Throwable e) {
      String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
      ClodMC.error(sender, "Internal command error: " + message);
      ClodMC.logException(e);
    }
    return true;
  }

  // args

  protected @NotNull String popArg(@NotNull List<String> args) {
    if (args.isEmpty()) {
      throw new CommandError(this.usageMessage);
    }
    return args.removeFirst();
  }

  protected @NotNull String popArg(@NotNull List<String> args, @NotNull String fallback) {
    if (args.isEmpty()) {
      return fallback;
    }
    return args.removeFirst();
  }

  protected @NotNull Player popPlayerArg(@NotNull List<String> args) {
    String name = this.popArg(args);
    Player player = Bukkit.getPlayerExact(name);
    if (player == null) {
      throw new CommandError("Invalid player: " + name);
    }
    return player;
  }

  // tab completion

  @Override
  public @NotNull List<String> tabComplete(
      @NotNull CommandSender sender, @NotNull String alias, @NotNull String @NotNull [] args)
      throws IllegalArgumentException {
    return List.of();
  }

  // helpers

  protected @NotNull Player toPlayer(@NotNull CommandSender sender) throws CommandError {
    if (!(sender instanceof Player player)) {
      throw new CommandError("This command can only be run by a player");
    }
    return player;
  }
}
