package au.com.glob.clodmc.modules.crafting;

import au.com.glob.clodmc.annotations.Audience;
import au.com.glob.clodmc.annotations.Doc;
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
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.jspecify.annotations.NullMarked;

@Doc(
    audience = Audience.PLAYER,
    title = "Chorus Flower Crafting",
    description = "Adds crafting recipes for Chorus Flower")
@NullMarked
public class ChorusFlower implements Module, Listener {
  private final ShapedRecipe flowerRecipe;
  private final ShapelessRecipe fruitRecipe;

  // register the chorus flower recipes
  public ChorusFlower() {
    this.flowerRecipe =
        new ShapedRecipe(
            new NamespacedKey("clod-mc", "chorus-flower"),
            new ItemStack(Material.CHORUS_FLOWER, 1));
    this.flowerRecipe.shape("CC", "CC");
    this.flowerRecipe.setIngredient('C', Material.CHORUS_FRUIT);
    Bukkit.addRecipe(this.flowerRecipe);

    this.fruitRecipe =
        new ShapelessRecipe(
            new NamespacedKey("clod-mc", "chorus-fruit"), new ItemStack(Material.CHORUS_FRUIT, 4));
    this.fruitRecipe.addIngredient(Material.CHORUS_FLOWER);
    Bukkit.addRecipe(this.fruitRecipe);
  }

  // auto-discover recipe when player joins if they have ingredients
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerJoin(final PlayerJoinEvent event) {
    Schedule.asynchronously(
        () -> {
          final Player player = event.getPlayer();
          if (!player.hasDiscoveredRecipe(this.flowerRecipe.getKey())
              && player.getInventory().contains(Material.CHORUS_FRUIT)) {
            Schedule.onMainThread(() -> player.discoverRecipe(this.flowerRecipe.getKey()));
          }
          if (!player.hasDiscoveredRecipe(this.fruitRecipe.getKey())
              && player.getInventory().contains(Material.CHORUS_FLOWER)) {
            Schedule.onMainThread(() -> player.discoverRecipe(this.fruitRecipe.getKey()));
          }
        });
  }

  // auto-discover recipe when player picks up required ingredients
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityPickupItem(final EntityPickupItemEvent event) {
    if (!(event.getEntity() instanceof final Player player)) {
      return;
    }
    final ItemStack item = event.getItem().getItemStack();
    if (!player.hasDiscoveredRecipe(this.flowerRecipe.getKey())
        && item.getType() == Material.CHORUS_FRUIT) {
      Schedule.onMainThread(() -> player.discoverRecipe(this.flowerRecipe.getKey()));
    }
    if (!player.hasDiscoveredRecipe(this.fruitRecipe.getKey())
        && item.getType() == Material.CHORUS_FLOWER) {
      Schedule.onMainThread(() -> player.discoverRecipe(this.fruitRecipe.getKey()));
    }
  }
}
