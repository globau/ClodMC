package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.ClodMC;
import java.util.Collection;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.TimeSkipEvent;

public class Sleep implements Listener {
  public static void register() {
    Bukkit.getServer().getPluginManager().registerEvents(new Sleep(), ClodMC.instance);
  }

  @EventHandler
  public void onTimeSkip(TimeSkipEvent event) {
    Collection<? extends Player> players = ClodMC.instance.getServer().getOnlinePlayers();
    List<String> sleeping =
        players.stream().filter(LivingEntity::isSleeping).map(Player::getName).toList();
    if (sleeping.isEmpty()) {
      return;
    }

    String message = "<grey>" + String.join(",", sleeping) + " skipped the night</grey>";
    for (Player player : players) {
      if (player.getWorld().getEnvironment() == World.Environment.NORMAL) {
        player.sendRichMessage(message);
      }
    }
  }
}
