package au.com.glob.clodmc.modules.crafting;

import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Schedule;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.jspecify.annotations.NullMarked;

/** Adds a crafting recipe for Spore Blossoms */
@NullMarked
public class SporeBlossom implements Module, Listener {
  private final ShapelessRecipe recipe;

  public SporeBlossom() {
    this.recipe =
        new ShapelessRecipe(
            new NamespacedKey("clod-mc", "spore-blossom"),
            new ItemStack(Material.SPORE_BLOSSOM, 1));
    this.recipe.addIngredient(Material.BIG_DRIPLEAF);
    this.recipe.addIngredient(Material.ALLIUM);
    Bukkit.addRecipe(this.recipe);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerJoin(PlayerJoinEvent event) {
    Schedule.asynchronously(
        () -> {
          Player player = event.getPlayer();
          if (player.hasDiscoveredRecipe(this.recipe.getKey())) {
            return;
          }

          for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.isEmpty()) {
              continue;
            }
            for (RecipeChoice choice : this.recipe.getChoiceList()) {
              if (choice.test(item)) {
                Schedule.onMainThread(() -> player.discoverRecipe(this.recipe.getKey()));
              }
            }
          }
        });
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityPickupItem(EntityPickupItemEvent event) {
    if (!(event.getEntity() instanceof Player player)) {
      return;
    }
    if (player.hasDiscoveredRecipe(this.recipe.getKey())) {
      return;
    }

    ItemStack item = event.getItem().getItemStack();
    for (RecipeChoice choice : this.recipe.getChoiceList()) {
      if (choice.test(item)) {
        Schedule.onMainThread(() -> player.discoverRecipe(this.recipe.getKey()));
      }
    }
  }
}
