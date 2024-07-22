package au.com.glob.clodmc.modules.gateways;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AnchorItem {
  protected static final @NotNull NamespacedKey RECIPE_KEY = new NamespacedKey("clod-mc", "anchor");
  private static final String @NotNull [] SHAPE = new String[] {"PWP", "EWE", "ERE"};
  private static final @NotNull Map<Character, Material> SHAPE_MATERIALS =
      Map.of(
          'P', Material.ENDER_PEARL,
          'W', Material.AIR,
          'E', Material.END_STONE,
          'R', Material.RESPAWN_ANCHOR);

  private static final @NotNull String DEFAULT_ANCHOR_NAME = "Gateway Anchor";

  private static final @NotNull NamespacedKey NETWORK_KEY = new NamespacedKey("clod-mc", "network");
  private static final @NotNull NamespacedKey TOP_KEY = new NamespacedKey("clod-mc", "network-top");
  private static final @NotNull NamespacedKey BOTTOM_KEY =
      new NamespacedKey("clod-mc", "network-bottom");

  protected static @NotNull ShapedRecipe getRecipe() {
    ShapedRecipe recipe = new ShapedRecipe(RECIPE_KEY, AnchorItem.create());
    recipe.shape(SHAPE);
    for (Map.Entry<Character, Material> entry : SHAPE_MATERIALS.entrySet()) {
      Material material = entry.getValue();
      if (material == Material.AIR) {
        recipe.setIngredient(entry.getKey(), new RecipeChoice.MaterialChoice(Colours.materials()));
      } else {
        recipe.setIngredient(entry.getKey(), material);
      }
    }
    return recipe;
  }

  protected static @NotNull ItemStack create() {
    ItemStack item = new ItemStack(Material.RESPAWN_ANCHOR);
    ItemMeta meta = item.getItemMeta();
    meta.displayName(Component.text(DEFAULT_ANCHOR_NAME));
    meta.setEnchantmentGlintOverride(true);
    meta.getPersistentDataContainer().set(RECIPE_KEY, PersistentDataType.BOOLEAN, true);
    item.setItemMeta(meta);
    return item;
  }

  protected static boolean isAnchor(@Nullable ItemStack item) {
    if (item == null) {
      return false;
    }
    ItemMeta meta = item.getItemMeta();
    return meta != null && meta.getPersistentDataContainer().has(RECIPE_KEY);
  }

  protected static int getNetworkId(@NotNull ItemStack item) {
    Integer networkId =
        item.getItemMeta()
            .getPersistentDataContainer()
            .get(NETWORK_KEY, PersistentDataType.INTEGER);
    if (networkId == null) {
      throw new RuntimeException("invalid anchor item state: malformed or missing network-id");
    }
    return networkId;
  }

  protected static @Nullable String getName(@NotNull ItemStack item) {
    Component displayName = item.getItemMeta().displayName();
    assert displayName != null;
    String plainTextName = PlainTextComponentSerializer.plainText().serialize(displayName);
    return plainTextName.equals(DEFAULT_ANCHOR_NAME) ? null : plainTextName;
  }

  protected static void setMeta(
      @NotNull ItemStack anchorItem, int networkId, @Nullable String name, boolean isDuplicate) {
    Colours.Network network = Colours.networkIdToColours(networkId);
    ItemMeta meta = anchorItem.getItemMeta();
    meta.displayName(Component.text(name == null ? DEFAULT_ANCHOR_NAME : name));
    List<TextComponent> lore =
        new ArrayList<>(List.of(network.top.getText(), network.bottom.getText()));
    if (isDuplicate) {
      lore.add(Component.text("(duplicate)"));
    }
    meta.lore(lore);
    PersistentDataContainer container = meta.getPersistentDataContainer();
    container.set(NETWORK_KEY, PersistentDataType.INTEGER, networkId);
    container.set(TOP_KEY, PersistentDataType.STRING, network.top.name);
    container.set(BOTTOM_KEY, PersistentDataType.STRING, network.bottom.name);
    anchorItem.setItemMeta(meta);
  }

  public static void setMeta(@NotNull ItemStack anchorItem, int networkId, boolean isDuplicate) {
    setMeta(anchorItem, networkId, null, isDuplicate);
  }

  public static void setMeta(@NotNull ItemStack anchorItem, boolean isDuplicate) {
    ItemMeta meta = anchorItem.getItemMeta();
    Integer networkIdBoxed =
        meta.getPersistentDataContainer().get(NETWORK_KEY, PersistentDataType.INTEGER);
    assert networkIdBoxed != null;
    setMeta(anchorItem, networkIdBoxed, null, isDuplicate);
  }
}
