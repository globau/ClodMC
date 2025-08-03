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

@NullMarked
class AnchorItem {
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

  static ShapedRecipe getRecipe() {
    Material[] materials =
        Colours.COLOURS.stream().map((Colour colour) -> colour.material).toArray(Material[]::new);

    ShapedRecipe recipe = new ShapedRecipe(RECIPE_KEY, AnchorItem.create());
    recipe.shape(SHAPE);
    for (Map.Entry<Character, Material> entry : SHAPE_MATERIALS.entrySet()) {
      Material material = entry.getValue();
      if (material == Material.AIR) {
        recipe.setIngredient(entry.getKey(), new RecipeChoice.MaterialChoice(materials));
      } else {
        recipe.setIngredient(entry.getKey(), material);
      }
    }
    return recipe;
  }

  static ItemStack create() {
    ItemStack item = new ItemStack(Material.RESPAWN_ANCHOR);
    ItemMeta meta = item.getItemMeta();
    meta.displayName(Component.text(DEFAULT_ANCHOR_NAME));
    meta.setEnchantmentGlintOverride(true);
    meta.getPersistentDataContainer().set(RECIPE_KEY, PersistentDataType.BOOLEAN, true);
    item.setItemMeta(meta);
    return item;
  }

  static boolean isAnchor(@Nullable ItemStack item) {
    if (item == null) {
      return false;
    }
    ItemMeta meta = item.getItemMeta();
    return meta != null && meta.getPersistentDataContainer().has(RECIPE_KEY);
  }

  static int getNetworkId(ItemStack item) {
    Integer networkId =
        item.getItemMeta()
            .getPersistentDataContainer()
            .get(NETWORK_KEY, PersistentDataType.INTEGER);
    if (networkId == null) {
      throw new RuntimeException("invalid anchor item state: malformed or missing network-id");
    }
    return networkId;
  }

  static @Nullable String getName(ItemStack item) {
    Component displayName = item.getItemMeta().displayName();
    if (displayName == null) {
      return null;
    }
    String plainTextName = StringUtil.asText(Objects.requireNonNull(displayName));
    return plainTextName.equals(DEFAULT_ANCHOR_NAME) ? null : plainTextName;
  }

  static void setMeta(
      ItemStack anchorItem, int networkId, @Nullable String name, @Nullable String suffix) {
    Network network = Network.of(networkId);
    ItemMeta meta = anchorItem.getItemMeta();
    meta.displayName(Component.text(name == null ? DEFAULT_ANCHOR_NAME : name));
    List<TextComponent> lore =
        new ArrayList<>(List.of(network.top.getText(), network.bottom.getText()));
    if (suffix != null) {
      lore.add(Component.text("(%s)".formatted(suffix)));
    }
    meta.lore(lore);
    PersistentDataContainer container = meta.getPersistentDataContainer();
    container.set(NETWORK_KEY, PersistentDataType.INTEGER, networkId);
    container.set(TOP_KEY, PersistentDataType.STRING, network.top.name);
    container.set(BOTTOM_KEY, PersistentDataType.STRING, network.bottom.name);
    anchorItem.setItemMeta(meta);
  }

  static void setMeta(ItemStack anchorItem, int networkId) {
    boolean isDuplicate =
        Gateways.instance.instances.values().stream()
            .anyMatch((AnchorBlock anchorBlock) -> anchorBlock.networkId == networkId);
    boolean isRandomDest = networkId == Gateways.RANDOM_NETWORK_ID;

    String suffix;
    if (isRandomDest) {
      suffix = "random";
    } else if (isDuplicate) {
      suffix = "duplicate";
    } else {
      suffix = null;
    }

    AnchorItem.setMeta(anchorItem, networkId, getName(anchorItem), suffix);
  }

  static void refreshMeta(ItemStack anchorItem) {
    setMeta(anchorItem, getNetworkId(anchorItem));
  }

  static void clearExtraMeta(ItemStack anchorItem) {
    ItemMeta meta = anchorItem.getItemMeta();
    Integer networkIdBoxed =
        meta.getPersistentDataContainer().get(NETWORK_KEY, PersistentDataType.INTEGER);
    setMeta(anchorItem, Objects.requireNonNull(networkIdBoxed), null, null);
  }
}
