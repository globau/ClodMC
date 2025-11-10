package au.com.glob.clodmc.modules.mobs;

import au.com.glob.clodmc.annotations.Audience;
import au.com.glob.clodmc.annotations.Doc;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.StringUtil;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

@Doc(
    audience = Audience.PLAYER,
    title = "Silence Mobs/Animals",
    description = "Include 🔇 when naming a mob/animal/etc to silence it")
@NullMarked
public class SilenceMobs implements Listener, Module {
  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onPlayerInteractAtEntity(final PlayerInteractAtEntityEvent event) {
    final ItemStack heldItem = event.getPlayer().getInventory().getItem(event.getHand());
    if (heldItem.getType() == Material.NAME_TAG) {
      event.getRightClicked().setSilent(StringUtil.asText(heldItem.displayName()).contains("🔇"));
    }
  }
}
