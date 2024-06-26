package au.com.glob.clodmc.modules.gateways;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.BlockPos;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class Gateways implements Module, Listener {
  private final File configFile = new File(ClodMC.instance.getDataFolder(), "gateways.yml");
  private final Map<BlockPos, AnchorBlock> instances = new HashMap<>();
  private final Map<Player, BlockPos> ignore = new HashMap<>();

  public Gateways() {
    // add recipe
    ShapedRecipe recipe = new ShapedRecipe(Config.recipeKey, this.createAnchorItem());
    recipe.shape(Config.SHAPE);
    for (Map.Entry<Character, Material> entry : Config.SHAPE_MATERIALS.entrySet()) {
      Material material = entry.getValue();
      if (material == Material.AIR) {
        recipe.setIngredient(
            entry.getKey(),
            new RecipeChoice.MaterialChoice(
                Config.NETWORK_CRAFT.keySet().toArray(new Material[0])));
      } else {
        recipe.setIngredient(entry.getKey(), material);
      }
    }
    Bukkit.addRecipe(recipe);

    this.load();
  }

  private void load() {
    this.instances.clear();
    this.ignore.clear();

    YamlConfiguration config = YamlConfiguration.loadConfiguration(this.configFile);
    List<?> rawList = config.getList("anchors");
    if (rawList == null) {
      return;
    }

    // load and connect
    Map<Integer, BlockPos> connections = new HashMap<>();
    for (Object obj : rawList) {
      if (obj instanceof AnchorBlock anchorBlock) {
        this.instances.put(anchorBlock.blockPos, anchorBlock);
        if (connections.containsKey(anchorBlock.networkId)) {
          AnchorBlock otherAnchorBlock = this.instances.get(connections.get(anchorBlock.networkId));
          anchorBlock.connectedTo = otherAnchorBlock;
          otherAnchorBlock.connectedTo = anchorBlock;
        } else {
          connections.put(anchorBlock.networkId, anchorBlock.blockPos);
        }
      }
    }

    // emit particles
    for (AnchorBlock anchorBlock : this.instances.values()) {
      anchorBlock.updateParticles();
    }
  }

  private void save() {
    YamlConfiguration config = new YamlConfiguration();
    config.set("anchors", new ArrayList<>(this.instances.values()));
    try {
      config.save(this.configFile);
    } catch (IOException e) {
      ClodMC.logError(this.configFile + ": save failed: " + e);
    }
  }

  private @NotNull ItemStack createAnchorItem() {
    ItemStack item = new ItemStack(Material.RESPAWN_ANCHOR);
    ItemMeta meta = item.getItemMeta();
    meta.displayName(Component.text("Gateway Anchor"));
    meta.setEnchantmentGlintOverride(true);
    meta.getPersistentDataContainer().set(Config.recipeKey, PersistentDataType.BOOLEAN, true);
    item.setItemMeta(meta);
    return item;
  }

  private void setAnchorItemMeta(@NotNull ItemStack anchorItem, int networkId) {
    String topColour = Config.idToTopName(networkId);
    String bottomColour = Config.idToBottomName(networkId);
    ItemMeta meta = anchorItem.getItemMeta();
    meta.lore(List.of(Component.text(topColour), Component.text(bottomColour)));
    meta.getPersistentDataContainer().set(Config.networkKey, PersistentDataType.INTEGER, networkId);
    meta.getPersistentDataContainer().set(Config.topKey, PersistentDataType.STRING, topColour);
    meta.getPersistentDataContainer()
        .set(Config.bottomKey, PersistentDataType.STRING, bottomColour);
    anchorItem.setItemMeta(meta);
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    event.getPlayer().discoverRecipe(Config.recipeKey);
  }

  @EventHandler
  public void onPrepareItemCraftEvent(PrepareItemCraftEvent event) {
    ItemStack item = event.getInventory().getResult();
    if (item == null || !AnchorBlock.isAnchor(item)) {
      return;
    }

    // add lore and metadata to crafted anchors
    String topColour = "";
    String bottomColour = "";
    for (ItemStack ingredient : event.getInventory().getMatrix()) {
      if (ingredient == null) {
        continue;
      }
      Material material = ingredient.getType();
      if (Config.NETWORK_CRAFT.containsKey(material)) {
        if (topColour.isEmpty()) {
          topColour = Config.NETWORK_CRAFT.get(material);
        } else {
          bottomColour = Config.NETWORK_CRAFT.get(material);
        }
      }
    }

    this.setAnchorItemMeta(item, Config.coloursToId(topColour, bottomColour));
    item.setAmount(2);
    event.getInventory().setResult(item);
  }

  @EventHandler
  public void onBlockPlaced(BlockPlaceEvent event) {
    // prevent placing blocks in the 2 blocks above an anchorBlock
    BlockPos below1Pos = BlockPos.of(event.getBlock().getLocation()).down();
    BlockPos below2Pos = below1Pos.down();
    if (this.instances.containsKey(below1Pos) || this.instances.containsKey(below2Pos)) {
      event.setCancelled(true);
      return;
    }

    // check if item being placed is an anchor block
    ItemMeta meta = event.getItemInHand().getItemMeta();
    if (meta == null || !meta.getPersistentDataContainer().has(Config.recipeKey)) {
      return;
    }

    // needs to be placed with two blocks of air above
    BlockPos above1Pos = BlockPos.of(event.getBlock().getLocation()).up();
    BlockPos above2Pos = above1Pos.up();
    if (!(above1Pos.getBlock().isEmpty() && above2Pos.getBlock().isEmpty())) {
      event.setCancelled(true);
      event.getPlayer().sendRichMessage("<yellow>Anchors require two air blocks above</yellow>");
      return;
    }

    // extract network id
    Integer networkIdBoxed =
        meta.getPersistentDataContainer().get(Config.networkKey, PersistentDataType.INTEGER);
    if (networkIdBoxed == null) {
      return;
    }
    int networkId = networkIdBoxed;

    // find connecting anchor block
    AnchorBlock otherAnchorBlock = null;
    int matching = 0;
    for (AnchorBlock a : this.instances.values()) {
      if (a.networkId == networkId) {
        otherAnchorBlock = a;
        matching++;
      }
    }

    if (matching > 1) {
      event.setCancelled(true);
      event.getPlayer().sendRichMessage("<yellow>Anchors already connected</yellow>");
      return;
    }

    // connect anchor
    AnchorBlock anchorBlock = new AnchorBlock(networkId, event.getBlock().getLocation());
    anchorBlock.connectedTo = otherAnchorBlock;
    anchorBlock.updateParticles();

    if (otherAnchorBlock != null) {
      otherAnchorBlock.connectedTo = anchorBlock;
      otherAnchorBlock.updateParticles();
    }

    // save
    this.instances.put(anchorBlock.blockPos, anchorBlock);
    this.save();
  }

  @EventHandler
  public void onEntityChangeBlock(EntityChangeBlockEvent event) {
    // if a falling entity turns into a block inside the gateway, break the block
    if (event.getEntity() instanceof FallingBlock fallingBlock) {
      BlockPos belowPos = BlockPos.of(event.getBlock().getLocation()).down();
      if (this.instances.containsKey(belowPos)) {
        ItemStack itemStack = new ItemStack(fallingBlock.getBlockData().getMaterial());
        event.getBlock().getWorld().dropItem(event.getBlock().getLocation(), itemStack);
        event.setCancelled(true);
      }
    }
  }

  @EventHandler
  public void onBlockBreak(BlockBreakEvent event) {
    BlockPos blockPos = BlockPos.of(event.getBlock().getLocation());
    AnchorBlock anchorBlock = this.instances.get(blockPos);
    if (anchorBlock == null) {
      return;
    }

    // remove portal and disconnect
    anchorBlock.stopParticles();
    this.instances.remove(blockPos);
    if (anchorBlock.connectedTo != null) {
      anchorBlock.connectedTo.connectedTo = null;
      anchorBlock.connectedTo.updateParticles();
    }
    this.save();

    // drop anchor block
    if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
      return;
    }
    event.setDropItems(false);

    ItemStack anchorItem = this.createAnchorItem();
    this.setAnchorItemMeta(anchorItem, anchorBlock.networkId);
    anchorBlock
        .blockPos
        .getWorld()
        .dropItemNaturally(anchorBlock.blockPos.asLocation(), anchorItem);
  }

  @EventHandler
  public void onPlayerMoveEvent(PlayerMoveEvent event) {
    Player player = event.getPlayer();
    Location playerLocation = player.getLocation();
    BlockPos playerPos = BlockPos.of(playerLocation);

    // ignore players after they teleport, until they step off the anchorblock
    if (this.ignore.containsKey(player)) {
      if (this.ignore.get(player).equals(playerPos)) {
        return;
      }
      this.ignore.remove(player);
    }

    // check for anchorblock
    BlockPos standingOnPos = BlockPos.of(playerLocation, 0, -1, 0);
    AnchorBlock anchorBlock = this.instances.get(standingOnPos);
    if (anchorBlock == null || anchorBlock.connectedTo == null) {
      return;
    }

    // show a message; normally this is only visible if the destination
    // chunk is slow to load
    player.showTitle(
        Title.title(
            Component.text(""),
            Component.text("Teleporting"),
            Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(500))));

    // teleport
    Location teleportPos = anchorBlock.connectedTo.teleportLocation(player);
    player
        .teleportAsync(teleportPos)
        .whenComplete(
            (Boolean result, Throwable e) -> {
              player.clearTitle();
              if (result != null && result) {
                this.ignore.put(player, BlockPos.of(teleportPos));
                player.playSound(teleportPos, Sound.ENTITY_PLAYER_TELEPORT, 1, 1);
              }
            });
  }

  @EventHandler
  public void onPlayerJoinEvent(PlayerJoinEvent event) {
    // if player spawns on an anchor block don't immediately teleport
    Player player = event.getPlayer();
    BlockPos standingOnPos = BlockPos.of(player.getLocation(), 0, -1, 0);
    AnchorBlock anchorBlock = this.instances.get(standingOnPos);
    if (anchorBlock != null && anchorBlock.connectedTo != null) {
      this.ignore.put(player, BlockPos.of(player.getLocation()));
    }
  }

  @EventHandler
  public void onPlayerQuitEvent(PlayerQuitEvent event) {
    this.ignore.remove(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPlayerInteractEvent(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
      return;
    }

    // prevent charging up the respawn anchor
    AnchorBlock anchorBlock =
        this.instances.get(BlockPos.of(event.getClickedBlock().getLocation()));
    event.setCancelled(anchorBlock != null);
  }
}
