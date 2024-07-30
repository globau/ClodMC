package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Chat;
import au.com.glob.clodmc.util.StringUtil;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.TimeSkipEvent;
import org.jetbrains.annotations.NotNull;

public class Sleep implements Listener, Module {
  @EventHandler
  public void onTimeSkip(@NotNull TimeSkipEvent event) {
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

    // notify players who slept the night
    // however, no need to tell the only player who slept
    for (Player player : players) {
      if (player.getWorld().getEnvironment() == World.Environment.NORMAL) {
        if (sleeping.size() > 1 || !sleeping.getFirst().equals(player.getName())) {
          Chat.fyi(player, StringUtil.joinComma(sleeping) + " skipped the night");
        }
      }
    }
  }
}
