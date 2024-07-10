package au.com.glob.clodmc.modules.welcome;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Config;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class WelcomeGift implements Listener, Module {
  public WelcomeGift() {
    super();
    Config.init("welcome-book.yml");
  }

  @EventHandler
  public void onJoin(@NotNull PlayerJoinEvent event) {
    new BukkitRunnable() {
      @Override
      public void run() {
        Player player = event.getPlayer();
        if (!player.hasPlayedBefore()) {
          ItemStack book = WelcomeBook.build();
          if (book != null) {
            player.getInventory().addItem(book);
          }
        }
      }
    }.runTaskLater(ClodMC.instance, 5);
  }
}
