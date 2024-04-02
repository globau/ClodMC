package au.com.glob.clodmc.command;

import au.com.glob.clodmc.config.PlayerConfig;
import au.com.glob.clodmc.config.PluginConfig;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public abstract class PlayerCommand extends BaseCommand {
  @Override
  public boolean onCommand(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String[] args) {
    try {
      if (!(sender instanceof Player player)) {
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
      return true;
    }
  }

  protected abstract void execute(
      @NotNull Player player, @NotNull PlayerConfig playerConfig, @NotNull String[] args)
      throws CommandError;

  protected abstract List<String> tabComplete(
      @NotNull Player player, @NotNull PlayerConfig playerConfig, @NotNull String[] args);
}
