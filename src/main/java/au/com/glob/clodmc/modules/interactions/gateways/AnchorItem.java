package au.com.glob.clodmc.modules.interactions.gateways;

import au.com.glob.clodmc.util.StringUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** manages the gateway anchor item recipe and creation */
@NullMarked
final class AnchorItem {
  static final NamespacedKey RECIPE_KEY = new NamespacedKey("clod-mc", "anchor");
  private static final String[] SHAPE = new String[] {"PWP", "EWE", "ERE"};
  private static final Map<Character, Material> SHAPE_MATERIALS =
      Map.of(
          'P', Material.ENDER_PEARL,
          'W', Material.AIR,
          'E', Material.END_STONE,
          'R', Material.RESPAWN_ANCHOR);

  private static final String DEFAULT_ANCHOR_NAME = "Gateway Anchor";

  private static final NamespacedKey NETWORK_KEY = new NamespacedKey("clod-mc", "network");
  private static final NamespacedKey TOP_KEY = new NamespacedKey("clod-mc", "network-top");
  private static final NamespacedKey BOTTOM_KEY = new NamespacedKey("clod-mc", "network-bottom");

  // creates the crafting recipe for gateway anchor item
  static ShapedRecipe getRecipe() {
    final Material[] materials =
        Colours.COLOURS.stream().map((Colour colour) -> colour.material).toArray(Material[]::new);

    final ShapedRecipe recipe = new ShapedRecipe(RECIPE_KEY, AnchorItem.create());
    recipe.shape(SHAPE);
    for (final Map.Entry<Character, Material> entry : SHAPE_MATERIALS.entrySet()) {
      final Material material = entry.getValue();
      if (material == Material.AIR) {
        recipe.setIngredient(entry.getKey(), new RecipeChoice.MaterialChoice(materials));
      } else {
        recipe.setIngredient(entry.getKey(), material);
      }
    }
    return recipe;
  }

  // creates a new gateway anchor item with default properties
  static ItemStack create() {
    final ItemStack item = new ItemStack(Material.RESPAWN_ANCHOR);
    final ItemMeta meta = item.getItemMeta();
    meta.displayName(Component.text(DEFAULT_ANCHOR_NAME));
    meta.setEnchantmentGlintOverride(true);
    meta.getPersistentDataContainer().set(RECIPE_KEY, PersistentDataType.BOOLEAN, true);
    item.setItemMeta(meta);
    return item;
  }

  // checks if the item is a gateway anchor
  static boolean isAnchor(@Nullable final ItemStack item) {
    if (item == null) {
      return false;
    }
    final ItemMeta meta = item.getItemMeta();
    return meta != null && meta.getPersistentDataContainer().has(RECIPE_KEY);
  }

  // gets the network id from anchor item
  static int getNetworkId(final ItemStack item) {
    final Integer networkId =
        item.getItemMeta()
            .getPersistentDataContainer()
            .get(NETWORK_KEY, PersistentDataType.INTEGER);
    if (networkId == null) {
      throw new RuntimeException("invalid anchor item state: malformed or missing network-id");
    }
    return networkId;
  }

  // gets the custom name from anchor item
  static @Nullable String getName(final ItemStack item) {
    final Component displayName = item.getItemMeta().displayName();
    if (displayName == null) {
      return null;
    }
    final String plainTextName = StringUtil.asText(Objects.requireNonNull(displayName));
    return plainTextName.equals(DEFAULT_ANCHOR_NAME) ? null : plainTextName;
  }

  // sets metadata on anchor item including network id, name and suffix
  static void setMeta(
      final ItemStack anchorItem,
      final int networkId,
      @Nullable final String name,
      @Nullable final String suffix) {
    final Network network = Network.of(networkId);
    final ItemMeta meta = anchorItem.getItemMeta();
    meta.displayName(Component.text(name == null ? DEFAULT_ANCHOR_NAME : name));
    final List<TextComponent> lore =
        new ArrayList<>(List.of(network.top.getText(), network.bottom.getText()));
    if (suffix != null) {
      lore.add(Component.text("(%s)".formatted(suffix)));
    }
    meta.lore(lore);
    final PersistentDataContainer container = meta.getPersistentDataContainer();
    container.set(NETWORK_KEY, PersistentDataType.INTEGER, networkId);
    container.set(TOP_KEY, PersistentDataType.STRING, network.top.name);
    container.set(BOTTOM_KEY, PersistentDataType.STRING, network.bottom.name);
    anchorItem.setItemMeta(meta);
  }

  // sets metadata on anchor item with duplicate and random detection
  static void setMeta(final ItemStack anchorItem, final int networkId) {
    final boolean isDuplicate =
        Gateways.instance.instances.values().stream()
            .anyMatch((AnchorBlock anchorBlock) -> anchorBlock.networkId == networkId);
    final boolean isRandomDest = networkId == Gateways.RANDOM_NETWORK_ID;

    final String suffix;
    if (isRandomDest) {
      suffix = "random";
    } else if (isDuplicate) {
      suffix = "duplicate";
    } else {
      suffix = null;
    }

    AnchorItem.setMeta(anchorItem, networkId, getName(anchorItem), suffix);
  }

  // refreshes metadata on existing anchor item
  static void refreshMeta(final ItemStack anchorItem) {
    setMeta(anchorItem, getNetworkId(anchorItem));
  }

  // clears extra metadata from anchor item after crafting
  static void clearExtraMeta(final ItemStack anchorItem) {
    final ItemMeta meta = anchorItem.getItemMeta();
    final Integer networkIdBoxed =
        meta.getPersistentDataContainer().get(NETWORK_KEY, PersistentDataType.INTEGER);
    setMeta(anchorItem, Objects.requireNonNull(networkIdBoxed), null, null);
  }
}
