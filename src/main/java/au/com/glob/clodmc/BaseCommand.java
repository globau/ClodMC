package au.com.glob.clodmc;

import au.com.glob.clodmc.config.PlayerConfig;
import au.com.glob.clodmc.config.PluginConfig;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
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
      if (!(sender instanceof org.bukkit.entity.Player player)) {
        throw new CommandError("This command can only be run by a player");
      }
      PlayerConfig playerConfig = PluginConfig.getInstance().getPlayerConfig(player);
      if (playerConfig == null) {
        throw new CommandError("Internal Error: no home data found");
      }
      this.execute(player, playerConfig, args);
      return true;
    } catch (CommandError e) {
      sender.sendRichMessage("<red>" + e.getMessage() + "</red>");
      return false;
    }
  }

  public @Nullable List<String> onTabComplete(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String[] args) {
    try {
      if (!(sender instanceof org.bukkit.entity.Player player)) {
        return null;
      }
      PlayerConfig playerConfig = PluginConfig.getInstance().getPlayerConfig(player);
      if (playerConfig == null) {
        return List.of();
      }
      return this.tabComplete(player, playerConfig, args);
    } catch (CommandError e) {
      sender.sendRichMessage("<red>" + e.getMessage() + "</red>");
      return null;
    }
  }

  protected List<String> tabCompleteHomes(
      @NotNull PlayerConfig playerConfig, @NotNull String[] args) {
    if (args.length == 0) {
      return null;
    }
    return args[0].isBlank() ? playerConfig.getHomes() : List.of();
  }

  protected abstract void execute(
      @NotNull Player player, @NotNull PlayerConfig playerConfig, @NotNull String[] args)
      throws CommandError;

  protected abstract List<String> tabComplete(
      @NotNull Player player, @NotNull PlayerConfig playerConfig, @NotNull String[] args)
      throws CommandError;
}
