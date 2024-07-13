package au.com.glob.clodmc.modules.homes;

import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.SimpleCommand;
import au.com.glob.clodmc.util.PlayerLocation;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HomesCommand extends SimpleCommand implements Module {
  public HomesCommand() {
    super("homes", "List homes");
  }

  @Override
  public void execute(@NotNull CommandSender sender, @NotNull List<String> args) {
    Player player = this.toPlayer(sender);
    Map<String, PlayerLocation> homes = Homes.instance.getHomes(player);

    if (homes.isEmpty()) {
      player.sendRichMessage("Homes: <italic>None</italic>");
    } else {
      StringJoiner joiner = new StringJoiner(", ");
      for (String name : homes.keySet().stream().sorted().toList()) {
        joiner.add(name);
      }
      player.sendMessage("Homes: " + joiner);
    }
  }
}
