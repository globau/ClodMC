package au.com.glob.clodmc.command;

import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BaseCommand implements CommandExecutor, TabCompleter {
  @Override
  public boolean onCommand(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String[] args) {
    try {
      this.execute(sender, args);
      return true;
    } catch (CommandError e) {
      sender.sendRichMessage("<red>" + e.getMessage() + "</red>");
      return true;
    }
  }

  @Override
  public @Nullable List<String> onTabComplete(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String[] args) {
    return List.of();
  }

  protected @Nullable List<String> completeFrom(
      @NotNull List<String> values, @NotNull String[] args) {
    if (args.length == 0) {
      return null;
    }
    String prefix = args[0];
    return values.stream().filter(v -> v.startsWith(prefix)).toList();
  }

  protected void execute(@NotNull CommandSender sender, @NotNull String[] args)
      throws CommandError {}
}
