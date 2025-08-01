package au.com.glob.clodmc.modules.mobs;

import au.com.glob.clodmc.modules.Module;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

/** Improve the drop rates of selected entities */
@NullMarked
public class BetterDrops implements Listener, Module {
  @SuppressWarnings("MissingCasesInEnumSwitch")
  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onEntityDeath(EntityDeathEvent event) {
    // adjust drops for mobs killed by players
    if (event.getEntity().getKiller() == null) {
      return;
    }

    switch (event.getEntityType()) {
      case SHULKER ->
          // shulkers should always drop two shells
          event
              .getDrops()
              .add(new ItemStack(Material.SHULKER_SHELL, event.getDrops().isEmpty() ? 2 : 1));
      case WITHER_SKELETON -> {
        // increase wither-skeleton head drop chance
        for (ItemStack drop : event.getDrops()) {
          if (drop.getType() == Material.WITHER_SKELETON_SKULL) {
            return;
          }
        }
        if (Math.random() < 0.2) {
          event.getDrops().add(new ItemStack(Material.WITHER_SKELETON_SKULL, 1));
        }
      }
    }
  }
}
