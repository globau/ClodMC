package au.com.glob.clodmc.modules.interactions.gateways;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.command.CommandBuilder;
import au.com.glob.clodmc.command.EitherCommandSender;
import au.com.glob.clodmc.datafile.PlayerDataFile;
import au.com.glob.clodmc.datafile.PlayerDataFiles;
import au.com.glob.clodmc.modules.Module;
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

/** Player built point-to-point teleports */
@NullMarked
public class Gateways implements Module, Listener {
  @SuppressWarnings({"NotNullFieldNotInitialized", "NullAway.Init"})
  static Gateways instance;

  private static final int MAX_RANDOM_TP_TIME = 60; // minutes
  private static final int MIN_RANDOM_TP_DISTANCE = 1500;
  private static final int RANDOM_TP_COOLDOWN = 60; // seconds
  static final int VISIBLE_RANGE_SQUARED = 16 * 16;

  private final File configFile = new File(ClodMC.instance.getDataFolder(), "gateways.yml");
  final Map<BlockPos, AnchorBlock> instances = new HashMap<>();
  private final Map<Player, BlockPos> ignore = new HashMap<>();
  private final Random random = new Random();

  // a black-black gateway teleports the player to a random location
  public static final int RANDOM_NETWORK_ID =
      coloursToNetworkId(
          Objects.requireNonNull(Colours.of(Material.BLACK_WOOL)),
          Objects.requireNonNull(Colours.of(Material.BLACK_WOOL)));

  public Gateways() {
    instance = this;

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
            });
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
          if (otherAnchorBlock != null) {
            anchorBlock.connectTo(otherAnchorBlock);
          }
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
      if (BlueMapGateways.instance != null) {
        BlueMapGateways.instance.update();
      }
    } catch (IOException e) {
      Logger.error(this.configFile + ": save failed: " + e);
    }
  }

  @EventHandler
  public void onPrepareItemCraft(PrepareItemCraftEvent event) {
    ItemStack item = event.getInventory().getResult();
    if (!AnchorItem.isAnchor(item)) {
      return;
    }

    @Nullable ItemStack[] matrix = event.getInventory().getMatrix();
    Colour topColour = Colours.of(matrix[1]);
    Colour bottomColour = Colours.of(matrix[4]);
    if (topColour == null || bottomColour == null) {
      Logger.error("failed to craft anchor block: invalid colour material");
      return;
    }
    int networkId = coloursToNetworkId(topColour, bottomColour);

    // add lore and metadata to crafted anchors
    boolean isDuplicate =
        this.instances.values().stream()
            .anyMatch((AnchorBlock anchorBlock) -> anchorBlock.networkId == networkId);
    boolean isRandomDest = networkId == RANDOM_NETWORK_ID;
    int amount = 2;
    if (isRandomDest && event.getView().getPlayer() instanceof Player player) {
      amount = player.isOp() ? 1 : 0;
    }

    String suffix;
    if (isRandomDest) {
      suffix = "random";
    } else if (isDuplicate) {
      suffix = "duplicate";
    } else {
      suffix = null;
    }
    AnchorItem.setMeta(item, networkId, null, suffix);
    item.setAmount(amount);

    event.getInventory().setResult(item);
  }

  @EventHandler
  public void onCraftItem(CraftItemEvent event) {
    ItemStack item = event.getCurrentItem();

    if (AnchorItem.isAnchor(item)) {
      AnchorItem.clearExtraMeta(item);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
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

    int networkId = AnchorItem.getNetworkId(item);
    AnchorBlock anchorBlock =
        new AnchorBlock(networkId, event.getBlock().getLocation(), AnchorItem.getName(item));

    if (!anchorBlock.isRandom) {
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
        Chat.error(event.getPlayer(), "Anchors already connected");
        return;
      }

      if (otherAnchorBlock == null) {
        anchorBlock.disconnect();
      } else {
        anchorBlock.connectTo(otherAnchorBlock);
        otherAnchorBlock.updateVisuals();
      }
    }

    anchorBlock.updateVisuals();

    // save
    this.instances.put(anchorBlock.blockPos, anchorBlock);
    this.save();
  }

  @EventHandler(ignoreCancelled = true)
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

  @EventHandler(ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    BlockPos blockPos = BlockPos.of(event.getBlock().getLocation());
    AnchorBlock anchorBlock = this.instances.get(blockPos);
    if (anchorBlock == null) {
      return;
    }

    // remove portal and disconnect
    anchorBlock.stopVisuals();
    this.instances.remove(blockPos);
    AnchorBlock otherAnchorBlock = anchorBlock.connectedTo;
    if (otherAnchorBlock != null) {
      otherAnchorBlock.disconnect();
      otherAnchorBlock.updateVisuals();
    }
    this.save();

    // drop anchor block
    if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
      return;
    }
    event.setDropItems(false);

    ItemStack anchorItem = AnchorItem.create();
    AnchorItem.setMeta(anchorItem, anchorBlock.networkId, anchorBlock.name, null);
    anchorBlock.blockPos.world.dropItem(anchorBlock.blockPos.asLocation(), anchorItem);
  }

  @EventHandler
  public void onPlayerMove(PlayerMoveEvent event) {
    Player player = event.getPlayer();

    // keep track of gateways within range of players
    if (event.hasChangedBlock()) {
      this.updateNearbyAnchors(player);
    }

    if (player.getGameMode().equals(GameMode.SPECTATOR)) {
      return;
    }

    // show colours of looked-at anchorblocks
    Block targetBlock = player.getTargetBlockExact(Players.INTERACTION_RANGE);
    if (targetBlock != null) {
      AnchorBlock anchorBlock = this.instances.get(BlockPos.of(targetBlock.getLocation()));
      if (anchorBlock != null) {
        player.sendActionBar(StringUtil.asComponent(anchorBlock.getInformation()));
      }
    }

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
    BlockPos standingOnPos = BlockPos.of(playerLocation).down();
    AnchorBlock anchorBlock = this.instances.get(standingOnPos);
    if (anchorBlock == null) {
      return;
    }

    Location teleportPos;
    PlayerTeleportEvent.TeleportCause cause;

    if (anchorBlock.networkId == RANDOM_NETWORK_ID) {
      teleportPos = playerLocation;

      // only new players can use a random gateway
      if (!player.isOp()) {
        int ticks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        long minutesPlayed = Math.round(ticks / 20.0 / 60.0);
        if (minutesPlayed > MAX_RANDOM_TP_TIME) {
          Chat.error(player, "New Players Only");
          return;
        }
      }

      // cooldown
      PlayerDataFile dataFile = PlayerDataFiles.of(player);
      LocalDateTime now = TimeUtil.localNow();
      LocalDateTime lastRandomTeleport = dataFile.getDateTime("tpr");
      if (lastRandomTeleport != null) {
        long secondsSinceRandomTeleport = Duration.between(lastRandomTeleport, now).toSeconds();
        if (secondsSinceRandomTeleport < RANDOM_TP_COOLDOWN) {
          this.ignore.put(player, anchorBlock.blockPos.up());
          Chat.warning(
              player,
              "You must wait at another "
                  + StringUtil.plural2(RANDOM_TP_COOLDOWN - secondsSinceRandomTeleport, "second")
                  + " before teleporting again");
          return;
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

      World world = Bukkit.getWorld("world");
      if (world == null) {
        this.ignore.put(player, anchorBlock.blockPos.up());
        return;
      }
      WorldBorder border = world.getWorldBorder();

      int attemptsLeft = 25;
      while (attemptsLeft > 0) {
        attemptsLeft--;

        // pick a random location
        double radius = border.getSize() / 2.0;
        Location center = border.getCenter();
        Location randomPos;
        do {
          double distance =
              MIN_RANDOM_TP_DISTANCE
                  + (this.random.nextDouble() * (radius - MIN_RANDOM_TP_DISTANCE));
          double angle = 2 * Math.PI * this.random.nextDouble();
          double x = center.getX() + distance * Math.cos(angle);
          double z = center.getZ() + distance * Math.sin(angle);
          double y = world.getHighestBlockYAt(new Location(world, x, 0, z));
          randomPos = new Location(world, x, y, z);
        } while (!border.isInside(randomPos));

        // avoid claims
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(randomPos, true, null);
        if (claim != null) {
          continue;
        }

        // find a safe location
        teleportPos = TeleportUtil.getSafePos(randomPos);
        String biomeKey = teleportPos.getBlock().getBiome().getKey().value();
        if (biomeKey.equals("ocean")
            || biomeKey.endsWith("_ocean")
            || biomeKey.equals("river")
            || biomeKey.endsWith("_river")) {
          continue;
        }

        // getSafePos can put a player into an unsafe location if there aren't any nearby
        if (!TeleportUtil.isUnsafe(teleportPos.getBlock())) {
          break;
        }
      }
      if (attemptsLeft == 0) {
        this.ignore.put(player, anchorBlock.blockPos.up());
        Chat.error(player, "Failed to find a safe location");
        return;
      }

      // set the cause as COMMAND to allow /back
      cause = PlayerTeleportEvent.TeleportCause.COMMAND;

      Chat.info(
          player,
          "Sending you "
              + String.format("%,d", Math.round(playerLocation.distance(teleportPos)))
              + " blocks away");

    } else {
      // teleport to connected anchor

      AnchorBlock connectedTo = anchorBlock.connectedTo;
      if (connectedTo == null) {
        player.sendActionBar(StringUtil.asComponent(anchorBlock.getInformation()));
        return;
      }

      teleportPos = connectedTo.teleportLocation(player);
      // set cause to PLUGIN so this teleport is ignored by /back
      cause = PlayerTeleportEvent.TeleportCause.PLUGIN;
    }

    // teleport
    this.ignore.put(player, anchorBlock.blockPos.up());
    Location finalTeleportPos = teleportPos;
    player
        .teleportAsync(teleportPos, cause)
        .whenComplete(
            (@Nullable Boolean result, Throwable e) -> {
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
            (Throwable ex) -> {
              Chat.error(player, "Teleport failed");
              return null;
            });
  }

  @EventHandler
  public void onPlayerTeleport(PlayerTeleportEvent event) {
    this.updateNearbyAnchors(event.getPlayer(), event.getTo());
  }

  @EventHandler
  public void onPlayerRespawn(PlayerRespawnEvent event) {
    this.updateNearbyAnchors(event.getPlayer(), event.getRespawnLocation());
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();

    // add recipe to book
    player.discoverRecipe(AnchorItem.RECIPE_KEY);

    // enable visuals for nearby anchors
    this.updateNearbyAnchors(player);

    // if player spawns on an anchor block don't immediately teleport
    BlockPos standingOnPos = BlockPos.of(player.getLocation()).down();
    AnchorBlock anchorBlock = this.instances.get(standingOnPos);
    if (anchorBlock != null && anchorBlock.connectedTo != null) {
      this.ignore.put(player, BlockPos.of(player.getLocation()));
    }
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();

    this.ignore.remove(player);

    for (AnchorBlock anchorBlock : this.instances.values()) {
      anchorBlock.removeNearbyPlayer(player);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
      return;
    }

    // prevent charging up the respawn anchor
    AnchorBlock anchorBlock =
        this.instances.get(BlockPos.of(event.getClickedBlock().getLocation()));
    event.setCancelled(anchorBlock != null);
  }

  public void updateNearbyAnchors(Player player, Location location) {
    for (AnchorBlock anchorBlock : this.instances.values()) {
      if (anchorBlock.canSeeVisualsFrom(location)) {
        anchorBlock.addNearbyPlayer(player);
      } else {
        anchorBlock.removeNearbyPlayer(player);
      }
    }
  }

  public void updateNearbyAnchors(Player player) {
    this.updateNearbyAnchors(player, player.getLocation());
  }

  public Collection<AnchorBlock> getAnchorBlocks() {
    return this.instances.values();
  }

  private static int coloursToNetworkId(Colour topColour, Colour bottomColour) {
    return (topColour.index << 4) | bottomColour.index;
  }

  static Network networkIdToColours(int networkId) {
    return new Network(networkId);
  }
}
