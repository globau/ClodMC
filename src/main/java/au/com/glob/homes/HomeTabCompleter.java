package au.com.glob.homes;

import au.com.glob.homes.config.PlayerConfig;
import au.com.glob.homes.config.PluginConfig;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HomeTabCompleter implements TabCompleter {
  @Override
  public @Nullable List<String> onTabComplete(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String[] args) {
    if (!(sender instanceof Player player)) {
      return null;
    }
    if (args.length == 0) {
      return null;
    }
    if (!args[0].isBlank()) {
      return List.of();
    }

    PlayerConfig playerConfig = PluginConfig.getInstance().getPlayerConfig(player);
    if (playerConfig == null) {
      return List.of();
    }

    return playerConfig.getHomes();
  }
}
