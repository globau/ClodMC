package au.com.glob.clodmc.modules.interactions.gateways;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.annotations.Audience;
import au.com.glob.clodmc.annotations.Doc;
import au.com.glob.clodmc.command.CommandBuilder;
import au.com.glob.clodmc.command.EitherCommandSender;
import au.com.glob.clodmc.datafile.PlayerDataFile;
import au.com.glob.clodmc.datafile.PlayerDataFiles;
import au.com.glob.clodmc.events.PlayerTargetBlockEvent;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.ActionBar;
import au.com.glob.clodmc.util.BlockPos;
import au.com.glob.clodmc.util.Chat;
import au.com.glob.clodmc.util.ConfigUtil;
import au.com.glob.clodmc.util.Logger;
import au.com.glob.clodmc.util.Players;
import au.com.glob.clodmc.util.Schedule;
import au.com.glob.clodmc.util.StringUtil;
import au.com.glob.clodmc.util.TeleportUtil;
import au.com.glob.clodmc.util.TimeUtil;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
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
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Player-built point-to-point teleportation system using coloured wool anchors.
 *
 * <p>Gateway system architecture:
 *
 * <ul>
 *   <li>anchor blocks: in-world anchor for gateways
 *   <li>anchor item: in-inventory or dropped anchor block
 *   <li>colour-based networking: two-colour combinations define teleport networks
 *   <li>network linking: gateways with same colour pair teleport between each other; only one pair
 *       of linked colours can be placed
 *   <li>special networks: black-black provides random wilderness teleportation
 * </ul>
 *
 * <p>Teleportation behaviour:
 *
 * <ul>
 *   <li>players enter gateway by walking onto anchor block
 *   <li>prevents immediate return teleportation with ignore system
 *   <li>BlueMap integration shows named anchor block locations on web map
 * </ul>
 *
 * <p>Data persistence managed through gateways.yml with automatic saving and loading. Includes
 * admin commands for listing active gateways and debugging.
 */
@Doc(
    audience = Audience.PLAYER,
    title = "Point-to-Point Gateways",
    description = "Player-built point-to-point teleportation system using coloured wool anchors")
@NullMarked
public class Gateways implements Module, Listener {
  @SuppressWarnings({"NotNullFieldNotInitialized", "NullAway.Init"})
  static Gateways instance;

  private static final int MAX_RANDOM_TP_TIME = 60; // minutes
  private static final int MIN_RANDOM_TP_DISTANCE = 1500;
  private static final int RANDOM_TP_COOLDOWN = 60; // seconds

  private final File configFile = new File(ClodMC.instance.getDataFolder(), "gateways.yml");
  Map<BlockPos, AnchorBlock> instances = new HashMap<>();
  private final Map<Player, BlockPos> ignore = new HashMap<>();
  private final Random random = new Random();

  // a black-black gateway teleports the player to a random location
  public static final int RANDOM_NETWORK_ID =
      Network.coloursToNetworkId(
          Objects.requireNonNull(Colours.of(Material.BLACK_WOOL)),
          Objects.requireNonNull(Colours.of(Material.BLACK_WOOL)));

  // initialise gateway system with recipes and commands
  public Gateways() {
    instance = this;

    ConfigurationSerialization.registerClass(AnchorBlock.class);
    Bukkit.addRecipe(AnchorItem.getRecipe());

    CommandBuilder.build("gateways")
        .description("List gateways in use")
        .executor(
            (final EitherCommandSender sender) -> {
              if (this.instances.isEmpty()) {
                Chat.warning(sender, "No gateways");
                return;
              }

              final String gateways =
                  this.instances.values().stream()
                      .map(AnchorBlock::getColourPair)
                      .distinct()
                      .sorted()
                      .collect(Collectors.joining(", "));
              Chat.info(sender, "Existing gateways: %s".formatted(gateways));
            });
  }

  // load gateway configuration from yaml file
  @Override
  public void loadConfig() {
    if (!ConfigUtil.sanityChecked) {
      Bukkit.shutdown();
      throw new RuntimeException("config file loaded before sanity checks");
    }

    this.instances.clear();
    this.ignore.clear();

    final YamlConfiguration config = YamlConfiguration.loadConfiguration(this.configFile);
    final List<?> rawList = config.getList("anchors");
    if (rawList == null) {
      return;
    }

    // load and connect
    final Map<Integer, BlockPos> connections = new HashMap<>();
    for (final Object obj : rawList) {
      if (obj instanceof final AnchorBlock anchorBlock) {
        this.instances.put(anchorBlock.blockPos, anchorBlock);
        if (connections.containsKey(anchorBlock.networkId)) {
          final AnchorBlock otherAnchorBlock =
              this.instances.get(connections.get(anchorBlock.networkId));
          if (otherAnchorBlock != null) {
            anchorBlock.connectTo(otherAnchorBlock);
          }
        } else {
          connections.put(anchorBlock.networkId, anchorBlock.blockPos);
        }
      }
    }

    // visuals
    for (final AnchorBlock anchorBlock : this.instances.values()) {
      anchorBlock.visuals.update();
    }
  }

  // save gateway configuration to yaml file
  private void save() {
    final YamlConfiguration config = new YamlConfiguration();
    config.set("anchors", new ArrayList<>(this.instances.values()));
    try {
      config.save(this.configFile);
      if (BlueMapGateways.instance != null) {
        BlueMapGateways.instance.update();
      }
    } catch (final IOException e) {
      Logger.error("failed to write %s".formatted(this.configFile), e);
    }
  }

  // set anchor item metadata during crafting
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPrepareItemCraft(final PrepareItemCraftEvent event) {
    final ItemStack item = event.getInventory().getResult();
    if (!AnchorItem.isAnchor(item)) {
      return;
    }

    @Nullable final ItemStack[] matrix = event.getInventory().getMatrix();
    final Colour topColour = Colours.of(matrix[1]);
    final Colour bottomColour = Colours.of(matrix[4]);
    if (topColour == null || bottomColour == null) {
      Logger.error("failed to craft anchor block: invalid colour material");
      return;
    }
    final int networkId = Network.coloursToNetworkId(topColour, bottomColour);

    AnchorItem.setMeta(item, networkId);

    int amount = 2;
    if (networkId == RANDOM_NETWORK_ID
        && event.getView().getPlayer() instanceof final Player player) {
      amount = player.isOp() ? 1 : 0;
    }
    item.setAmount(amount);

    event.getInventory().setResult(item);
  }

  // clean up extra metadata after crafting
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onCraftItem(final CraftItemEvent event) {
    final ItemStack item = event.getCurrentItem();

    if (AnchorItem.isAnchor(item)) {
      AnchorItem.clearExtraMeta(item);
    }
  }

  // refresh anchor item metadata in anvil
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPrepareAnvil(final PrepareAnvilEvent event) {
    final ItemStack item = event.getResult();
    if (AnchorItem.isAnchor(item)) {
      AnchorItem.refreshMeta(item);
    }
  }

  // handle anchor block placement and network connections
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onBlockPlace(final BlockPlaceEvent event) {
    // prevent placing blocks in the 2 blocks above an anchorBlock
    final BlockPos below1Pos = BlockPos.of(event.getBlock().getLocation()).down();
    final BlockPos below2Pos = below1Pos.down();
    if (this.instances.containsKey(below1Pos) || this.instances.containsKey(below2Pos)) {
      event.setCancelled(true);
      return;
    }

    final ItemStack item = event.getItemInHand();
    if (!AnchorItem.isAnchor(item)) {
      return;
    }

    // needs to be placed with two blocks of air above
    final BlockPos above1Pos = BlockPos.of(event.getBlock().getLocation()).up();
    final BlockPos above2Pos = above1Pos.up();
    if (!(above1Pos.getBlock().isEmpty() && above2Pos.getBlock().isEmpty())) {
      event.setCancelled(true);
      Chat.error(event.getPlayer(), "Anchors require two air blocks above");
      return;
    }

    final int networkId = AnchorItem.getNetworkId(item);
    final AnchorBlock anchorBlock =
        new AnchorBlock(networkId, event.getBlock().getLocation(), AnchorItem.getName(item));

    if (!anchorBlock.isRandom) {
      // find connecting anchor block
      AnchorBlock otherAnchorBlock = null;
      int matching = 0;
      for (final AnchorBlock a : this.instances.values()) {
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

      if (otherAnchorBlock == null) {
        anchorBlock.disconnect();
      } else {
        anchorBlock.connectTo(otherAnchorBlock);
        otherAnchorBlock.visuals.update();
      }
    }

    anchorBlock.visuals.update();

    // save
    this.instances.put(anchorBlock.blockPos, anchorBlock);
    this.save();
  }

  // prevent falling blocks from interfering with gateways
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onEntityChangeBlock(final EntityChangeBlockEvent event) {
    // if a falling entity turns into a block inside the gateway, break the block
    if (event.getEntity() instanceof final FallingBlock fallingBlock) {
      final BlockPos belowPos = BlockPos.of(event.getBlock().getLocation()).down();
      if (this.instances.containsKey(belowPos)) {
        final ItemStack itemStack = new ItemStack(fallingBlock.getBlockData().getMaterial());
        event.getBlock().getWorld().dropItem(event.getBlock().getLocation(), itemStack);
        event.setCancelled(true);
      }
    }
  }

  // handle anchor block removal and drop anchor item
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onBlockBreak(final BlockBreakEvent event) {
    final BlockPos blockPos = BlockPos.of(event.getBlock().getLocation());
    final AnchorBlock anchorBlock = this.instances.get(blockPos);
    if (anchorBlock == null) {
      return;
    }

    // remove portal and disconnect
    anchorBlock.visuals.disable();
    this.instances.remove(blockPos);
    final AnchorBlock otherAnchorBlock = anchorBlock.connectedTo;
    if (otherAnchorBlock != null) {
      otherAnchorBlock.disconnect();
      otherAnchorBlock.visuals.update();
    }
    this.save();

    // drop anchor block
    if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
      return;
    }
    event.setDropItems(false);

    final ItemStack anchorItem = AnchorItem.create();
    AnchorItem.setMeta(anchorItem, anchorBlock.networkId, anchorBlock.name, null);
    anchorBlock.blockPos.world.dropItem(anchorBlock.blockPos.asLocation(), anchorItem);
  }

  // show info when a player looks at a gateway
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerTargetBlock(final PlayerTargetBlockEvent event) {
    final Player player = event.getPlayer();
    final Block block = event.getTargetBlock();

    if (block == null) {
      return;
    }

    final AnchorBlock anchorBlock = this.instances.get(BlockPos.of(block.getLocation()));
    if (anchorBlock != null) {
      ActionBar.plain(player, anchorBlock.getInformation());
    }
  }

  // handle player movement and gateway interactions
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerMove(final PlayerMoveEvent event) {
    final Player player = event.getPlayer();

    // don't teleport spectators, and only need to check if player's block changes
    if (player.getGameMode().equals(GameMode.SPECTATOR) || !event.hasChangedBlock()) {
      return;
    }

    // keep track of gateways within range of players
    this.updateNearbyAnchors(player);

    final Location playerLocation = event.getTo();
    final BlockPos playerPos = BlockPos.of(playerLocation);

    // ignore players after they teleport, until they step off the anchorblock
    if (this.ignore.containsKey(player)) {
      if (this.ignore.get(player).equals(playerPos)) {
        return;
      }
      this.ignore.remove(player);
    }

    // check for anchorblock
    final BlockPos standingOnPos = BlockPos.of(playerLocation).down();
    final AnchorBlock anchorBlock = this.instances.get(standingOnPos);
    if (anchorBlock == null) {
      return;
    }

    final Location teleportPos;
    final PlayerTeleportEvent.TeleportCause cause;

    if (anchorBlock.networkId == RANDOM_NETWORK_ID) {
      teleportPos = this.randomTeleportLocation(anchorBlock, player);
      if (teleportPos == null) {
        return;
      }
      // set the cause as COMMAND to allow /back
      cause = PlayerTeleportEvent.TeleportCause.COMMAND;

    } else {
      // teleport to connected anchor

      final AnchorBlock connectedTo = anchorBlock.connectedTo;
      if (connectedTo == null) {
        ActionBar.plain(player, anchorBlock.getInformation());
        return;
      }

      teleportPos = connectedTo.teleportLocation(player);
      // set cause to PLUGIN so this teleport is ignored by /back
      cause = PlayerTeleportEvent.TeleportCause.PLUGIN;
    }

    // teleport
    this.ignore.put(player, anchorBlock.blockPos.up());
    final Location finalTeleportPos = teleportPos;
    player
        .teleportAsync(teleportPos, cause)
        .whenComplete(
            (@Nullable final Boolean result, final Throwable e) -> {
              player.clearTitle();
              if (result != null && result) {
                this.ignore.put(player, BlockPos.of(finalTeleportPos));
                if (Players.isBedrock(player)) {
                  // delay playing the sound by a tick to work around a Geyser issue
                  Schedule.delayed(
                      1,
                      () -> player.playSound(finalTeleportPos, Sound.ENTITY_PLAYER_TELEPORT, 1, 1));
                } else {
                  player.playSound(finalTeleportPos, Sound.ENTITY_PLAYER_TELEPORT, 1, 1);
                }
                player
                    .getLocation()
                    .getWorld()
                    .playEffect(player.getLocation(), Effect.ENDER_SIGNAL, 0);
              }
            })
        .exceptionally(
            (final Throwable ex) -> {
              Chat.error(player, "Teleport failed");
              return null;
            });
  }

  // update nearby anchor visuals after teleportation
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerTeleport(final PlayerTeleportEvent event) {
    this.updateNearbyAnchors(event.getPlayer(), event.getTo());
  }

  // update nearby anchor visuals after respawn
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerRespawn(final PlayerRespawnEvent event) {
    this.updateNearbyAnchors(event.getPlayer(), event.getRespawnLocation());
  }

  // setup gateway visuals and recipes for joining player
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerJoin(final PlayerJoinEvent event) {
    final Player player = event.getPlayer();

    // add recipe to book
    player.discoverRecipe(AnchorItem.RECIPE_KEY);

    // enable visuals for nearby anchors
    this.updateNearbyAnchors(player);

    // if player spawns on an anchor block don't immediately teleport
    final BlockPos standingOnPos = BlockPos.of(player.getLocation()).down();
    final AnchorBlock anchorBlock = this.instances.get(standingOnPos);
    if (anchorBlock != null && anchorBlock.connectedTo != null) {
      this.ignore.put(player, BlockPos.of(player.getLocation()));
    }
  }

  // clean up player data when they quit
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerQuit(final PlayerQuitEvent event) {
    final Player player = event.getPlayer();

    this.ignore.remove(player);

    for (final AnchorBlock anchorBlock : this.instances.values()) {
      anchorBlock.visuals.removeNearbyPlayer(player);
    }
  }

  // prevent charging respawn anchors used as gateways
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onPlayerInteract(final PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
      return;
    }

    // prevent charging up the respawn anchor
    final AnchorBlock anchorBlock =
        this.instances.get(BlockPos.of(event.getClickedBlock().getLocation()));
    event.setCancelled(anchorBlock != null);
  }

  // update visual effects for all anchors near player location
  public void updateNearbyAnchors(final Player player, final Location location) {
    for (final AnchorBlock anchorBlock : this.instances.values()) {
      anchorBlock.visuals.updateNearbyPlayer(player, location);
    }
  }

  // update visual effects for all anchors near player
  public void updateNearbyAnchors(final Player player) {
    this.updateNearbyAnchors(player, player.getLocation());
  }

  // get all active anchor blocks for bluemap integration
  public Collection<AnchorBlock> getAnchorBlocks() {
    return this.instances.values();
  }

  // find safe random teleport location for new players
  private @Nullable Location randomTeleportLocation(
      final AnchorBlock anchorBlock, final Player player) {
    // only new players can use a random gateway
    if (!player.isOp()) {
      final int ticks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
      final long minutesPlayed = Math.round(ticks / 20.0 / 60.0);
      if (minutesPlayed > MAX_RANDOM_TP_TIME) {
        Chat.error(player, "New Players Only");
        return null;
      }
    }

    // cooldown
    final PlayerDataFile dataFile = PlayerDataFiles.of(player);
    final LocalDateTime now = TimeUtil.localNow();
    final LocalDateTime lastRandomTeleport = dataFile.getDateTime("tpr");
    if (lastRandomTeleport != null) {
      final long secondsSinceRandomTeleport = Duration.between(lastRandomTeleport, now).toSeconds();
      if (secondsSinceRandomTeleport < RANDOM_TP_COOLDOWN) {
        this.ignore.put(player, anchorBlock.blockPos.up());
        Chat.warning(
            player,
            "You must wait another %s before teleporting again"
                .formatted(
                    StringUtil.plural2(RANDOM_TP_COOLDOWN - secondsSinceRandomTeleport, "second")));
        return null;
      }
    }
    dataFile.setDateTime("tpr", now);
    dataFile.save();

    // show a message; normally this is only visible if the destination
    // chunk is slow to load
    player.showTitle(
        Title.title(
            Component.text(""),
            Component.text("Teleporting"),
            Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(500))));

    final World world = Bukkit.getWorld("world");
    if (world == null) {
      this.ignore.put(player, anchorBlock.blockPos.up());
      return null;
    }
    final WorldBorder border = world.getWorldBorder();

    int attemptsLeft = 25;
    while (attemptsLeft > 0) {
      attemptsLeft--;

      // pick a random location
      final double radius = border.getSize() / 2.0;
      final Location center = border.getCenter();
      Location randomPos;
      do {
        final double distance =
            MIN_RANDOM_TP_DISTANCE + (this.random.nextDouble() * (radius - MIN_RANDOM_TP_DISTANCE));
        final double angle = 2 * Math.PI * this.random.nextDouble();
        final double x = center.getX() + distance * Math.cos(angle);
        final double z = center.getZ() + distance * Math.sin(angle);
        final double y = world.getHighestBlockYAt(new Location(world, x, 0, z));
        randomPos = new Location(world, x, y, z);
      } while (!border.isInside(randomPos));

      // avoid claims
      final Claim claim = GriefPrevention.instance.dataStore.getClaimAt(randomPos, true, null);
      if (claim != null) {
        continue;
      }

      // find a safe location
      final Location teleportPos = TeleportUtil.getSafePos(randomPos);
      final String biomeKey = teleportPos.getBlock().getBiome().getKey().value();
      if (biomeKey.equals("ocean")
          || biomeKey.endsWith("_ocean")
          || biomeKey.equals("river")
          || biomeKey.endsWith("_river")) {
        continue;
      }

      // getSafePos can put a player into an unsafe location if there aren't any safe positions
      // nearby, which is normally fine but should be avoided here
      if (TeleportUtil.isUnsafe(teleportPos.getBlock())) {
        continue;
      }

      Chat.info(
          player,
          "Sending you %s blocks away"
              .formatted(
                  String.format("%,d", Math.round(player.getLocation().distance(teleportPos)))));
      return teleportPos;
    }
    this.ignore.put(player, anchorBlock.blockPos.up());
    Chat.error(player, "Failed to find a safe location");
    return null;
  }
}
