package au.com.glob.clodmc.modules.gateways;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.command.CommandBuilder;
import au.com.glob.clodmc.command.EitherCommandSender;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.bluemap.BlueMapUpdateEvent;
import au.com.glob.clodmc.util.BlockPos;
import au.com.glob.clodmc.util.Chat;
import au.com.glob.clodmc.util.ConfigUtil;
import au.com.glob.clodmc.util.Logger;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/** Player built point-to-point teleporters */
public class Gateways implements Module, Listener {
  private final @NotNull File configFile =
      new File(ClodMC.instance.getDataFolder(), "gateways.yml");
  private final @NotNull Map<BlockPos, AnchorBlock> instances = new HashMap<>();
  private final @NotNull Map<Player, BlockPos> ignore = new HashMap<>();

  public Gateways() {
    ConfigurationSerialization.registerClass(AnchorBlock.class);
    Bukkit.addRecipe(AnchorItem.getRecipe());

    CommandBuilder.build("gateways")
        .description("List gateway in use")
        .executor(
            (EitherCommandSender sender) -> {
              if (this.instances.isEmpty()) {
                Chat.warning(sender, "No gateways");
                return;
              }

              String gateways =
                  this.instances.values().stream()
                      .map(AnchorBlock::getColourPair)
                      .distinct()
                      .sorted()
                      .collect(Collectors.joining(", "));
              Chat.info(sender, "Existing gateways: " + gateways);
            })
        .register();
  }

  @Override
  public void loadConfig() {
    if (!ConfigUtil.sanityChecked) {
      Bukkit.shutdown();
      throw new RuntimeException("config file loaded before sanity checks");
    }

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
      anchorBlock.updateVisuals();
    }
  }

  private void save() {
    YamlConfiguration config = new YamlConfiguration();
    config.set("anchors", new ArrayList<>(this.instances.values()));
    try {
      config.save(this.configFile);

      Bukkit.getServer().getPluginManager().callEvent(new BlueMapUpdateEvent(Gateways.class));
    } catch (IOException e) {
      Logger.error(this.configFile + ": save failed: " + e);
    }
  }

  @EventHandler
  public void onPrepareItemCraft(@NotNull PrepareItemCraftEvent event) {
    ItemStack item = event.getInventory().getResult();
    if (!AnchorItem.isAnchor(item)) {
      return;
    }

    // add lore and metadata to crafted anchors
    ItemStack[] matrix = event.getInventory().getMatrix();
    Colours.Colour topColour = Colours.of(matrix[1]);
    Colours.Colour bottomColour = Colours.of(matrix[4]);
    if (topColour == null || bottomColour == null) {
      Logger.error("failed to craft anchor block: invalid colour material");
      return;
    }
    int networkId = Colours.coloursToNetworkId(topColour, bottomColour);
    boolean isDuplicate =
        this.instances.values().stream()
            .anyMatch((AnchorBlock anchorBlock) -> anchorBlock.networkId == networkId);

    AnchorItem.setMeta(item, networkId, isDuplicate);
    item.setAmount(2);
    event.getInventory().setResult(item);
  }

  @EventHandler
  public void onCraftItem(@NotNull CraftItemEvent event) {
    ItemStack item = event.getCurrentItem();

    if (AnchorItem.isAnchor(item)) {
      // remove duplicate indicator
      AnchorItem.setMeta(item, false);
    }
  }

  @EventHandler
  public void onBlockPlace(@NotNull BlockPlaceEvent event) {
    // prevent placing blocks in the 2 blocks above an anchorBlock
    BlockPos below1Pos = BlockPos.of(event.getBlock().getLocation()).down();
    BlockPos below2Pos = below1Pos.down();
    if (this.instances.containsKey(below1Pos) || this.instances.containsKey(below2Pos)) {
      event.setCancelled(true);
      return;
    }

    ItemStack item = event.getItemInHand();
    if (!AnchorItem.isAnchor(item)) {
      return;
    }

    // needs to be placed with two blocks of air above
    BlockPos above1Pos = BlockPos.of(event.getBlock().getLocation()).up();
    BlockPos above2Pos = above1Pos.up();
    if (!(above1Pos.getBlock().isEmpty() && above2Pos.getBlock().isEmpty())) {
      event.setCancelled(true);
      Chat.error(event.getPlayer(), "Anchors require two air blocks above");
      return;
    }

    // find connecting anchor block
    int networkId = AnchorItem.getNetworkId(item);
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
      Chat.error(event.getPlayer(), "Anchors already connected");
      return;
    }

    // connect anchor
    AnchorBlock anchorBlock =
        new AnchorBlock(networkId, event.getBlock().getLocation(), AnchorItem.getName(item));
    anchorBlock.connectedTo = otherAnchorBlock;
    anchorBlock.updateVisuals();

    if (otherAnchorBlock != null) {
      otherAnchorBlock.connectedTo = anchorBlock;
      otherAnchorBlock.updateVisuals();
    }

    // save
    this.instances.put(anchorBlock.blockPos, anchorBlock);
    this.save();
  }

  @EventHandler
  public void onEntityChangeBlock(@NotNull EntityChangeBlockEvent event) {
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
  public void onBlockBreak(@NotNull BlockBreakEvent event) {
    BlockPos blockPos = BlockPos.of(event.getBlock().getLocation());
    AnchorBlock anchorBlock = this.instances.get(blockPos);
    if (anchorBlock == null) {
      return;
    }

    // remove portal and disconnect
    anchorBlock.stopVisuals();
    this.instances.remove(blockPos);
    if (anchorBlock.connectedTo != null) {
      anchorBlock.connectedTo.connectedTo = null;
      anchorBlock.connectedTo.updateVisuals();
    }
    this.save();

    // drop anchor block
    if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
      return;
    }
    event.setDropItems(false);

    ItemStack anchorItem = AnchorItem.create();
    AnchorItem.setMeta(anchorItem, anchorBlock.networkId, anchorBlock.name, false);
    anchorBlock.blockPos.getWorld().dropItem(anchorBlock.blockPos.asLocation(), anchorItem);
  }

  @EventHandler
  public void onPlayerMove(@NotNull PlayerMoveEvent event) {
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
    if (anchorBlock == null) {
      return;
    }

    // if standing on a disconnected anchor, show the colour
    if (anchorBlock.connectedTo == null) {
      player.sendActionBar(
          MiniMessage.miniMessage()
              .deserialize("<yellow>" + anchorBlock.displayName + "</yellow> is not connected"));
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
        .teleportAsync(teleportPos, PlayerTeleportEvent.TeleportCause.PLUGIN)
        .whenComplete(
            (Boolean result, Throwable e) -> {
              player.clearTitle();
              if (result != null && result) {
                this.ignore.put(player, BlockPos.of(teleportPos));
                player.playSound(teleportPos, Sound.ENTITY_PLAYER_TELEPORT, 1, 1);
                player
                    .getLocation()
                    .getWorld()
                    .playEffect(player.getLocation(), Effect.ENDER_SIGNAL, 0);
              }
            });
  }

  @EventHandler
  public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
    event.getPlayer().discoverRecipe(AnchorItem.RECIPE_KEY);

    // if player spawns on an anchor block don't immediately teleport
    Player player = event.getPlayer();
    BlockPos standingOnPos = BlockPos.of(player.getLocation(), 0, -1, 0);
    AnchorBlock anchorBlock = this.instances.get(standingOnPos);
    if (anchorBlock != null && anchorBlock.connectedTo != null) {
      this.ignore.put(player, BlockPos.of(player.getLocation()));
    }
  }

  @EventHandler
  public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
    this.ignore.remove(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPlayerInteract(@NotNull PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
      return;
    }

    // prevent charging up the respawn anchor
    AnchorBlock anchorBlock =
        this.instances.get(BlockPos.of(event.getClickedBlock().getLocation()));
    event.setCancelled(anchorBlock != null);
  }

  public @NotNull Collection<AnchorBlock> getAnchorBlocks() {
    return this.instances.values();
  }
}
