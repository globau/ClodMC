package au.com.glob.homes.commands;

import au.com.glob.homes.BaseCommand;
import au.com.glob.homes.config.PlayerConfig;
import java.util.List;
import java.util.StringJoiner;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HomesCommand extends BaseCommand {
  @Override
  protected void execute(
      @NotNull Player player, @NotNull PlayerConfig playerConfig, @NotNull String[] args) {
    List<String> homes = playerConfig.getHomes();
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
}
