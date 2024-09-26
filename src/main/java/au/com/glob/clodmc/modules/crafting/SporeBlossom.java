package au.com.glob.clodmc.modules.crafting;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.Module;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.jetbrains.annotations.NotNull;

public class SporeBlossom implements Module, Listener {
  private final @NotNull ShapelessRecipe recipe;

  public SporeBlossom() {
    this.recipe =
        new ShapelessRecipe(
            new NamespacedKey("clod-mc", "spore-blossom"),
            new ItemStack(Material.SPORE_BLOSSOM, 1));
    this.recipe.addIngredient(Material.BIG_DRIPLEAF);
    this.recipe.addIngredient(Material.ALLIUM);
    Bukkit.addRecipe(this.recipe);
  }

  @EventHandler
  public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
    Bukkit.getScheduler()
        .runTaskAsynchronously(
            ClodMC.instance,
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
                    Bukkit.getScheduler()
                        .callSyncMethod(
                            ClodMC.instance, () -> player.discoverRecipe(this.recipe.getKey()));
                  }
                }
              }
            });
  }

  @EventHandler(ignoreCancelled = true)
  public void onPickupItem(@NotNull EntityPickupItemEvent event) {
    if (!(event.getEntity() instanceof Player player)) {
      return;
    }
    if (player.hasDiscoveredRecipe(this.recipe.getKey())) {
      return;
    }

    ItemStack item = event.getItem().getItemStack();
    for (RecipeChoice choice : this.recipe.getChoiceList()) {
      if (choice.test(item)) {
        Bukkit.getScheduler()
            .callSyncMethod(ClodMC.instance, () -> player.discoverRecipe(this.recipe.getKey()));
      }
    }
  }
}
