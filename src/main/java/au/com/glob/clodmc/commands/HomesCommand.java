package au.com.glob.clodmc.commands;

import au.com.glob.clodmc.BaseCommand;
import au.com.glob.clodmc.config.PlayerConfig;
import java.util.List;
import java.util.StringJoiner;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HomesCommand extends BaseCommand {
  @Override
  protected void execute(
      @NotNull Player player, @NotNull PlayerConfig playerConfig, @NotNull String[] args) {
    List<String> homes = playerConfig.getHomeNames();
    if (homes.isEmpty()) {
      player.sendRichMessage("Homes: <italic>None</italic>");
    } else {
      StringJoiner joiner = new StringJoiner(", ");
      for (String name : homes) {
        joiner.add(name);
      }
      player.sendMessage("Homes: " + joiner);
    }
  }

  @Override
  protected List<String> tabComplete(
      @NotNull Player player, @NotNull PlayerConfig playerConfig, @NotNull String[] args) {
    return List.of();
  }
}
