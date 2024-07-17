package au.com.glob.clodmc.modules.interactions;

import au.com.glob.clodmc.modules.Module;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class Decorations implements Module, Listener {
  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (event.getAction() == Action.PHYSICAL) {
      Block block = event.getClickedBlock();
      // pressure plates on top of trapdoors are purely decorative, cancel interaction
      if (block != null
          && Tag.PRESSURE_PLATES.isTagged(block.getType())
          && Tag.TRAPDOORS.isTagged(block.getRelative(BlockFace.DOWN).getType())) {
        event.setCancelled(true);
      }
    }
  }
}
