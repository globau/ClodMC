package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.Module;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.TimeSkipEvent;

public class Sleep implements Listener, Module {
  @EventHandler
  public void onTimeSkip(TimeSkipEvent event) {
    Collection<? extends Player> players = ClodMC.instance.getServer().getOnlinePlayers();
    List<String> sleeping =
        new java.util.ArrayList<>(
            players.stream()
                .filter(LivingEntity::isSleeping)
                .map(Player::getName)
                .sorted(Comparator.naturalOrder())
                .toList());
    if (sleeping.isEmpty()) {
      return;
    }

    String names;
    if (sleeping.size() == 1) {
      names = sleeping.getFirst();
    } else if (sleeping.size() == 2) {
      names = String.join(" and ", sleeping);
    } else {
      names =
          String.join(
              ", and ",
              List.of(
                  String.join(", ", sleeping.subList(0, sleeping.size() - 1)), sleeping.getLast()));
    }
    String message = "<grey>" + names + " skipped the night</grey>";

    for (Player player : players) {
      if (player.getWorld().getEnvironment() == World.Environment.NORMAL) {
        player.sendRichMessage(message);
      }
    }
  }
}
