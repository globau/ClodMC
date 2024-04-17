package au.com.glob.clodmc.modules.welcome;

import au.com.glob.clodmc.ClodMC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class WelcomeGift implements Listener {
  // give new players the welcome book

  public static void register() {
    Bukkit.getServer().getPluginManager().registerEvents(new WelcomeGift(), ClodMC.instance);
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
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
