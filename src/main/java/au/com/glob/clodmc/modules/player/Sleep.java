package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.annotations.Audience;
import au.com.glob.clodmc.annotations.Doc;
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
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.TimeSkipEvent;
import org.jspecify.annotations.NullMarked;

@Doc(
    audience = Audience.PLAYER,
    title = "Sleep",
    description = "Tell players who slept and skipped the night")
@NullMarked
public class Sleep implements Listener, Module {
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onTimeSkip(final TimeSkipEvent event) {
    final Collection<? extends Player> players = ClodMC.instance.getServer().getOnlinePlayers();
    final List<String> sleeping =
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
    for (final Player player : players) {
      if (player.getWorld().getEnvironment() == World.Environment.NORMAL) {
        if (sleeping.size() > 1 || !sleeping.getFirst().equals(player.getName())) {
          Chat.fyi(player, "%s skipped the night".formatted(StringUtil.joinComma(sleeping)));
        }
      }
    }
  }
}
