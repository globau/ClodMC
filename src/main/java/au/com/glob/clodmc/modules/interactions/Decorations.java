package au.com.glob.clodmc.modules.interactions;

import au.com.glob.clodmc.modules.Module;
import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

public class Decorations implements Module, Listener {
  private static final @NotNull Set<Material> PRESSURE_PLATES =
      EnumSet.of(
          Material.ACACIA_PRESSURE_PLATE,
          Material.BIRCH_PRESSURE_PLATE,
          Material.POLISHED_BLACKSTONE_PRESSURE_PLATE,
          Material.CRIMSON_PRESSURE_PLATE,
          Material.DARK_OAK_PRESSURE_PLATE,
          Material.LIGHT_WEIGHTED_PRESSURE_PLATE,
          Material.HEAVY_WEIGHTED_PRESSURE_PLATE,
          Material.MANGROVE_PRESSURE_PLATE,
          Material.JUNGLE_PRESSURE_PLATE,
          Material.OAK_PRESSURE_PLATE,
          Material.SPRUCE_PRESSURE_PLATE,
          Material.STONE_PRESSURE_PLATE,
          Material.WARPED_PRESSURE_PLATE);

  private static final @NotNull Set<Material> TRAPDOORS =
      EnumSet.of(
          Material.ACACIA_TRAPDOOR,
          Material.BAMBOO_TRAPDOOR,
          Material.BIRCH_TRAPDOOR,
          Material.CHERRY_TRAPDOOR,
          Material.CRIMSON_TRAPDOOR,
          Material.DARK_OAK_TRAPDOOR,
          Material.JUNGLE_TRAPDOOR,
          Material.MANGROVE_TRAPDOOR,
          Material.OAK_TRAPDOOR,
          Material.SPRUCE_TRAPDOOR,
          Material.WARPED_TRAPDOOR,
          Material.COPPER_TRAPDOOR,
          Material.EXPOSED_COPPER_TRAPDOOR,
          Material.WEATHERED_COPPER_TRAPDOOR,
          Material.OXIDIZED_COPPER_TRAPDOOR,
          Material.WAXED_COPPER_TRAPDOOR,
          Material.WAXED_EXPOSED_COPPER_TRAPDOOR,
          Material.WAXED_WEATHERED_COPPER_TRAPDOOR,
          Material.WAXED_OXIDIZED_COPPER_TRAPDOOR,
          Material.TRAPPED_CHEST,
          Material.IRON_TRAPDOOR);

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (event.getAction() == Action.PHYSICAL) {
      Block block = event.getClickedBlock();
      // pressure plates on top of trapdoors are purely decorative, cancel interaction
      if (block != null && PRESSURE_PLATES.contains(block.getType())) {
        Block below = block.getRelative(BlockFace.DOWN);
        if (TRAPDOORS.contains(below.getType())) {
          event.setCancelled(true);
        }
      }
    }
  }
}
