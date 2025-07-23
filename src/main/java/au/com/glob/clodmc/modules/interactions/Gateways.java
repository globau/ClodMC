package au.com.glob.clodmc.modules.interactions;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.command.CommandBuilder;
import au.com.glob.clodmc.command.EitherCommandSender;
import au.com.glob.clodmc.datafile.PlayerDataFile;
import au.com.glob.clodmc.datafile.PlayerDataFiles;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.bluemap.BlueMap;
import au.com.glob.clodmc.util.Chat;
import au.com.glob.clodmc.util.ConfigUtil;
import au.com.glob.clodmc.util.Logger;
import au.com.glob.clodmc.util.Players;
import au.com.glob.clodmc.util.Schedule;
import au.com.glob.clodmc.util.StringUtil;
import au.com.glob.clodmc.util.TeleportUtil;
import au.com.glob.clodmc.util.TimeUtil;
import com.destroystokyo.paper.ParticleBuilder;
import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Light;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.configuration.serialization.SerializableAs;
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
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.NumberConversions;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Player built point-to-point teleports */
@NullMarked
public class Gateways implements Module, Listener {
  @SuppressWarnings({"NotNullFieldNotInitialized", "NullAway.Init"})
  private static Gateways instance;

  private static @Nullable BlueMapGateways blueMapGateways;

  private static final int MAX_RANDOM_TP_TIME = 60; // minutes
  private static final int MIN_RANDOM_TP_DISTANCE = 1500;
  private static final int RANDOM_TP_COOLDOWN = 60; // seconds
  private static final int VISIBLE_RANGE_SQUARED = 16 * 16;

  private final File configFile = new File(ClodMC.instance.getDataFolder(), "gateways.yml");
  private final Map<BlockPos, AnchorBlock> instances = new HashMap<>();
  private final Map<Player, BlockPos> ignore = new HashMap<>();
  private final Random random = new Random();

  private static final List<Colour> COLOURS =
      List.of(
          new Colour(Material.WHITE_WOOL, "white", 0, Color.fromRGB(0xf9ffff)),
          new Colour(Material.ORANGE_WOOL, "orange", 1, Color.fromRGB(0xf9801d)),
          new Colour(Material.MAGENTA_WOOL, "magenta", 2, Color.fromRGB(0xc64fbd)),
          new Colour(Material.LIGHT_BLUE_WOOL, "light_blue", 3, Color.fromRGB(0x3ab3da)),
          new Colour(Material.YELLOW_WOOL, "yellow", 4, Color.fromRGB(0xffd83d)),
          new Colour(Material.LIME_WOOL, "lime", 5, Color.fromRGB(0x80c71f)),
          new Colour(Material.PINK_WOOL, "pink", 6, Color.fromRGB(0xf38caa)),
          new Colour(Material.GRAY_WOOL, "gray", 7, Color.fromRGB(0x474f52)),
          new Colour(Material.LIGHT_GRAY_WOOL, "light_gray", 8, Color.fromRGB(0x9c9d97)),
          new Colour(Material.CYAN_WOOL, "cyan", 9, Color.fromRGB(0x169c9d)),
          new Colour(Material.PURPLE_WOOL, "purple", 10, Color.fromRGB(0x8932b7)),
          new Colour(Material.BLUE_WOOL, "blue", 11, Color.fromRGB(0x3c44a9)),
          new Colour(Material.BROWN_WOOL, "brown", 12, Color.fromRGB(0x825432)),
          new Colour(Material.GREEN_WOOL, "green", 13, Color.fromRGB(0x5d7c15)),
          new Colour(Material.RED_WOOL, "red", 14, Color.fromRGB(0xb02e26)),
          new Colour(Material.BLACK_WOOL, "black", 15, Color.fromRGB(0x1d1c21)));

  // a black-black gateway teleports the player to a random location
  public static final int RANDOM_NETWORK_ID =
      coloursToNetworkId(
          Objects.requireNonNull(Colour.of(15)), Objects.requireNonNull(Colour.of(15)));

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
      if (blueMapGateways != null) {
        blueMapGateways.update();
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
    Colour topColour = Colour.of(matrix[1]);
    Colour bottomColour = Colour.of(matrix[4]);
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
    anchorBlock.blockPos.world().dropItem(anchorBlock.blockPos.asLocation(), anchorItem);
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

  private static Network networkIdToColours(int networkId) {
    return new Network(networkId);
  }

  //

  @SerializableAs("ClodMC.AnchorBlock")
  public static class AnchorBlock implements ConfigurationSerializable {
    private static final double EFFECT_RADIUS = 0.375;
    private static final double EFFECT_SPEED = 0.1;
    private static final int EFFECT_PARTICLES = 4;

    private final int networkId;
    private final BlockPos blockPos;
    private final @Nullable String name;
    private final String displayName;

    private final Location topLocation;
    private final Location bottomLocation;
    private final Colour topColour;
    private final Colour bottomColour;

    private @Nullable AnchorBlock connectedTo = null;
    private @Nullable BukkitTask particleTask = null;
    private final List<NearbyPlayer> nearbyPlayers = new ArrayList<>();
    private final boolean isRandom;

    public AnchorBlock(int networkId, Location location, @Nullable String name) {
      this.networkId = networkId;
      this.blockPos = BlockPos.of(location);
      this.name = name;

      this.topLocation = location.clone().add(0.5, 2.5, 0.5);
      this.bottomLocation = location.clone().add(0.5, 1.5, 0.5);

      Network network = networkIdToColours(networkId);
      this.topColour = network.top;
      this.bottomColour = network.bottom;

      this.displayName = this.getColourPair() + (this.name == null ? "" : " (" + this.name + ")");
      this.isRandom = networkId == RANDOM_NETWORK_ID;
    }

    @Override
    public String toString() {
      return "AnchorBlock{"
          + "blockPos="
          + this.blockPos
          + ", topColour="
          + this.topColour
          + ", bottomColour="
          + this.bottomColour
          + ", connected="
          + (this.connectedTo != null)
          + '}';
    }

    private String getInformation() {
      String prefix = "<yellow>" + this.displayName + "</yellow> - ";
      if (this.connectedTo != null) {
        return prefix
            + this.connectedTo.blockPos.getString(
                !this.blockPos.world.equals(this.connectedTo.blockPos.world));
      }
      if (this.networkId == RANDOM_NETWORK_ID) {
        return prefix + "Random Location";
      }
      return prefix + "Disconnected";
    }

    private String getColourPair() {
      return this.topColour.getDisplayName() + " :: " + this.bottomColour.getDisplayName();
    }

    private void connectTo(AnchorBlock otherBlock) {
      this.connectedTo = otherBlock;
      otherBlock.connectedTo = this;
    }

    private void disconnect() {
      if (this.connectedTo != null) {
        this.connectedTo.connectedTo = null;
      }
      this.connectedTo = null;
    }

    private Location facingLocation(Location location) {
      double yawRadians = Math.toRadians(location.getYaw());
      double facingX = location.getX() - Math.sin(yawRadians);
      double facingZ = location.getZ() + Math.cos(yawRadians);
      return new Location(location.getWorld(), facingX, location.getY(), facingZ);
    }

    private Block facingBlock(Location location) {
      return this.facingLocation(location).getBlock();
    }

    private boolean isFacingAnchor(Location location) {
      return Gateways.instance.instances.containsKey(BlockPos.of(this.facingLocation(location)));
    }

    private boolean isFacingAir(Location location) {
      return this.facingBlock(location).isEmpty();
    }

    private boolean isFacingSolid(Location location) {
      return this.facingBlock(location).isSolid();
    }

    private Location teleportLocation(Player player) {
      // rotate player to avoid facing a wall

      // get standing-on block, bottom, and top blocks for the player's location
      // snapped to 90 degrees of rotation
      Location blockLoc = this.blockPos.asLocation();
      blockLoc.setYaw((float) Math.round(player.getLocation().getYaw() / 90.0) * 90);
      blockLoc.setPitch(player.getLocation().getPitch());
      Location bottomLoc = blockLoc.clone().add(0, 1, 0);
      Location topLoc = bottomLoc.clone().add(0, 1, 0);

      // check for air; treat air blocks above other anchors as solid
      int attempts = 1;
      while (attempts <= 4
          && !(this.isFacingAir(bottomLoc)
              && this.isFacingAir(topLoc)
              && !this.isFacingAnchor(blockLoc))) {
        blockLoc.setYaw(((blockLoc.getYaw() + 90) + 180) % 360 - 180);
        bottomLoc.setYaw(blockLoc.getYaw());
        topLoc.setYaw(blockLoc.getYaw());
        attempts++;
      }

      // didn't find air, try again without the anchor check
      if (!(this.isFacingAir(bottomLoc)
          && this.isFacingAir(topLoc)
          && !this.isFacingAnchor(blockLoc))) {
        attempts = 1;
        while (attempts <= 4 && !(this.isFacingAir(bottomLoc) && this.isFacingAir(topLoc))) {
          bottomLoc.setYaw(((bottomLoc.getYaw() + 90) + 180) % 360 - 180);
          topLoc.setYaw(bottomLoc.getYaw());
          attempts++;
        }
      }

      // didn't find air, settle for non-solid
      if (!(this.isFacingAir(bottomLoc) && this.isFacingAir(topLoc))) {
        attempts = 1;
        while (attempts <= 4 && (this.isFacingSolid(bottomLoc) || this.isFacingSolid(topLoc))) {
          bottomLoc.setYaw(((bottomLoc.getYaw() + 90) + 180) % 360 - 180);
          topLoc.setYaw(bottomLoc.getYaw());
          attempts++;
        }
      }

      return bottomLoc;
    }

    private void updateVisuals() {
      boolean isActive = this.isRandom || this.connectedTo != null;

      this.updateLights(isActive ? 12 : 0);

      // re-calculate nearby players
      this.nearbyPlayers.clear();
      for (Player player : Bukkit.getOnlinePlayers()) {
        if (this.canSeeVisualsFrom(player.getLocation())) {
          this.addNearbyPlayer(player);
        }
      }

      if (this.particleTask != null) {
        this.particleTask.cancel();
      }
      this.particleTask =
          Schedule.periodically(
              2,
              () -> {
                if (!this.nearbyPlayers.isEmpty()) {
                  this.spawnJavaParticles(
                      this.nearbyPlayers.stream()
                          .filter(NearbyPlayer::isJava)
                          .map(NearbyPlayer::getPlayer)
                          .toList(),
                      isActive);
                  this.spawnBedrockParticles(
                      this.nearbyPlayers.stream()
                          .filter(NearbyPlayer::isBedrock)
                          .map(NearbyPlayer::getPlayer)
                          .toList());
                }
              });
    }

    private void spawnJavaParticles(Collection<Player> players, boolean isActive) {
      double baseRotation = this.blockPos.world().getGameTime() * EFFECT_SPEED;
      double angleStep = 2 * Math.PI / EFFECT_PARTICLES;
      int ringsPerSection = isActive ? 8 : 4;

      // reuse location objects to reduce gc pressure
      World world = this.blockPos.world();
      Location bottomParticleLoc = new Location(world, 0, 0, 0);
      Location topParticleLoc = new Location(world, 0, 0, 0);

      for (int ring = 0; ring < ringsPerSection; ring++) {
        double ringFraction = (double) ring / ringsPerSection;
        double bottomY = this.bottomLocation.getY() + ringFraction - 0.5;
        double topY = this.topLocation.getY() + ringFraction - 0.5;
        double ringRotation = baseRotation + (ring * 0.2);

        for (int i = 0; i < EFFECT_PARTICLES; i++) {
          double angle = ringRotation + (i * angleStep);
          double cosAngle = Math.cos(angle);
          double sinAngle = Math.sin(angle);

          // bottom particle
          double x = this.bottomLocation.getX() + EFFECT_RADIUS * cosAngle;
          double z = this.bottomLocation.getZ() + EFFECT_RADIUS * sinAngle;
          bottomParticleLoc.setX(x);
          bottomParticleLoc.setY(bottomY);
          bottomParticleLoc.setZ(z);
          new ParticleBuilder(Particle.TRAIL)
              .data(new Particle.Trail(bottomParticleLoc, this.bottomColour.color, 5))
              .location(bottomParticleLoc)
              .receivers(players)
              .count(1)
              .spawn();

          // top particle
          x = this.topLocation.getX() + EFFECT_RADIUS * cosAngle;
          z = this.topLocation.getZ() + EFFECT_RADIUS * sinAngle;
          topParticleLoc.setX(x);
          topParticleLoc.setY(topY);
          topParticleLoc.setZ(z);
          new ParticleBuilder(Particle.TRAIL)
              .data(new Particle.Trail(topParticleLoc, this.topColour.color, 5))
              .location(topParticleLoc)
              .receivers(players)
              .count(1)
              .spawn();
        }
      }
    }

    private void spawnBedrockParticles(List<Player> players) {
      new ParticleBuilder(Particle.DUST)
          .data(new Particle.DustOptions(this.topColour.color, 1))
          .location(this.topLocation)
          .receivers(players)
          .count(1)
          .spawn();
      new ParticleBuilder(Particle.DUST)
          .data(new Particle.DustOptions(this.bottomColour.color, 1))
          .location(this.bottomLocation)
          .receivers(players)
          .count(1)
          .spawn();
    }

    private void updateLights(int lightLevel) {
      World world = this.blockPos.world();
      for (Location loc : List.of(this.topLocation, this.bottomLocation)) {
        Block block = world.getBlockAt(loc);
        if (lightLevel == 0) {
          block.setType(Material.AIR);
        } else {
          block.setType(Material.LIGHT);
          Light light = (Light) block.getBlockData();
          light.setLevel(lightLevel);
          block.setBlockData(light);
        }
      }
    }

    private boolean canSeeVisualsFrom(Location playerLoc) {
      return this.bottomLocation.getWorld() == playerLoc.getWorld()
          && this.bottomLocation.distanceSquared(playerLoc) <= VISIBLE_RANGE_SQUARED
          && !(playerLoc.getBlockX() == this.bottomLocation.getBlockX()
              && playerLoc.getBlockY() == this.bottomLocation.getBlockY()
              && playerLoc.getBlockZ() == this.bottomLocation.getBlockZ());
    }

    private void addNearbyPlayer(Player player) {
      NearbyPlayer nearbyPlayer = new NearbyPlayer(player);
      if (!this.nearbyPlayers.contains(nearbyPlayer)) {
        this.nearbyPlayers.add(nearbyPlayer);
      }
    }

    private void removeNearbyPlayer(Player player) {
      Iterator<NearbyPlayer> iter = this.nearbyPlayers.iterator();
      while (iter.hasNext()) {
        if (iter.next().player.equals(player)) {
          iter.remove();
          break;
        }
      }
    }

    private void stopVisuals() {
      if (this.particleTask != null) {
        this.particleTask.cancel();
        this.particleTask = null;
      }
      this.updateLights(0);
    }

    @Override
    public Map<String, Object> serialize() {
      // note: doesn't store adjustY
      Map<String, Object> serialised = new HashMap<>();
      serialised.put("title", this.displayName);
      serialised.put("world", this.blockPos.world().getName());
      serialised.put("x", this.blockPos.x());
      serialised.put("y", this.blockPos.y());
      serialised.put("z", this.blockPos.z());
      serialised.put("id", this.networkId);
      if (this.name != null) {
        serialised.put("name", this.name);
      }
      return serialised;
    }

    @SuppressWarnings("unused")
    public static AnchorBlock deserialize(Map<String, Object> args) {
      World world = Bukkit.getWorld((String) args.get("world"));
      if (world == null) {
        throw new IllegalArgumentException("unknown world");
      }

      return new AnchorBlock(
          NumberConversions.toInt(args.get("id")),
          new Location(
              world,
              NumberConversions.toInt(args.get("x")),
              NumberConversions.toInt(args.get("y")),
              NumberConversions.toInt(args.get("z"))),
          (String) args.get("name"));
    }
  }

  //

  private static class AnchorItem {
    private static final NamespacedKey RECIPE_KEY = new NamespacedKey("clod-mc", "anchor");
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

    private static ShapedRecipe getRecipe() {
      Material[] materials =
          COLOURS.stream().map((Colour colour) -> colour.material).toArray(Material[]::new);

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

    private static ItemStack create() {
      ItemStack item = new ItemStack(Material.RESPAWN_ANCHOR);
      ItemMeta meta = item.getItemMeta();
      meta.displayName(Component.text(DEFAULT_ANCHOR_NAME));
      meta.setEnchantmentGlintOverride(true);
      meta.getPersistentDataContainer().set(RECIPE_KEY, PersistentDataType.BOOLEAN, true);
      item.setItemMeta(meta);
      return item;
    }

    private static boolean isAnchor(@Nullable ItemStack item) {
      if (item == null) {
        return false;
      }
      ItemMeta meta = item.getItemMeta();
      return meta != null && meta.getPersistentDataContainer().has(RECIPE_KEY);
    }

    private static int getNetworkId(ItemStack item) {
      Integer networkId =
          item.getItemMeta()
              .getPersistentDataContainer()
              .get(NETWORK_KEY, PersistentDataType.INTEGER);
      if (networkId == null) {
        throw new RuntimeException("invalid anchor item state: malformed or missing network-id");
      }
      return networkId;
    }

    private static @Nullable String getName(ItemStack item) {
      Component displayName = item.getItemMeta().displayName();
      String plainTextName = StringUtil.asText(Objects.requireNonNull(displayName));
      return plainTextName.equals(DEFAULT_ANCHOR_NAME) ? null : plainTextName;
    }

    private static void setMeta(
        ItemStack anchorItem, int networkId, @Nullable String name, @Nullable String suffix) {
      Network network = networkIdToColours(networkId);
      ItemMeta meta = anchorItem.getItemMeta();
      meta.displayName(Component.text(name == null ? DEFAULT_ANCHOR_NAME : name));
      List<TextComponent> lore =
          new ArrayList<>(List.of(network.top.getText(), network.bottom.getText()));
      if (suffix != null) {
        lore.add(Component.text("(" + suffix + ")"));
      }
      meta.lore(lore);
      PersistentDataContainer container = meta.getPersistentDataContainer();
      container.set(NETWORK_KEY, PersistentDataType.INTEGER, networkId);
      container.set(TOP_KEY, PersistentDataType.STRING, network.top.name);
      container.set(BOTTOM_KEY, PersistentDataType.STRING, network.bottom.name);
      anchorItem.setItemMeta(meta);
    }

    private static void clearExtraMeta(ItemStack anchorItem) {
      ItemMeta meta = anchorItem.getItemMeta();
      Integer networkIdBoxed =
          meta.getPersistentDataContainer().get(NETWORK_KEY, PersistentDataType.INTEGER);
      setMeta(anchorItem, Objects.requireNonNull(networkIdBoxed), null, null);
    }
  }

  //

  private record Colour(Material material, String name, int index, Color color) {
    @Override
    public String toString() {
      return this.name;
    }

    private String getDisplayName() {
      return StringUtil.toTitleCase(this.name.replace('_', ' '));
    }

    private TextComponent getText() {
      return Component.text(this.getDisplayName());
    }

    private static @Nullable Colour of(int index) {
      for (Colour colour : COLOURS) {
        if (colour.index == index) {
          return colour;
        }
      }
      return null;
    }

    private static @Nullable Colour of(@Nullable ItemStack item) {
      if (item == null) {
        return null;
      }
      Material material = item.getType();
      for (Colour colour : COLOURS) {
        if (colour.material == material) {
          return colour;
        }
      }
      return null;
    }
  }

  //

  private static class Network {
    private final Gateways.Colour top;
    private final Gateways.Colour bottom;

    private Network(int networkId) {
      Colour topColour = Colour.of((networkId >> 4) & 0x0F);
      Colour bottomColour = Colour.of(networkId & 0x0F);
      if (topColour == null || bottomColour == null) {
        throw new RuntimeException("malformed anchor networkID: " + networkId);
      }
      this.top = topColour;
      this.bottom = bottomColour;
    }
  }

  public static class BlueMapGateways extends BlueMap.Addon {
    private static final String MARKER_FILENAME = "gateway.svg";

    private final Map<World, MarkerSet> markerSets = new HashMap<>(3);

    public BlueMapGateways(BlueMapAPI api) {
      super(api);
      blueMapGateways = this;

      // create svg
      Path gatewayFilePath =
          api.getWebApp().getWebRoot().resolve("assets").resolve(MARKER_FILENAME);
      try {
        Files.createDirectories(gatewayFilePath.getParent());
        try (OutputStream out = Files.newOutputStream(gatewayFilePath)) {
          InputStream svgStream = ClodMC.instance.getResource(MARKER_FILENAME);
          Objects.requireNonNull(svgStream).transferTo(out);
        }
      } catch (IOException e) {
        Logger.error("failed to create " + gatewayFilePath + ": " + e);
      }

      // create markers
      for (World world : Bukkit.getWorlds()) {
        this.markerSets.put(
            world, MarkerSet.builder().label("Gateways").defaultHidden(false).build());
      }
    }

    @Override
    public void update() {
      for (MarkerSet markerSet : this.markerSets.values()) {
        markerSet.getMarkers().clear();
      }

      Set<String> seenColours = new HashSet<>();
      for (AnchorBlock anchorBlock : Gateways.instance.getAnchorBlocks()) {
        if (anchorBlock.name == null) {
          continue;
        }

        String id = "gw-" + anchorBlock.topColour.name + "-" + anchorBlock.bottomColour.name + "-";
        id = id + (seenColours.contains(id) ? "b" : "a");
        seenColours.add(id);

        Objects.requireNonNull(this.markerSets.get(anchorBlock.blockPos.world()))
            .getMarkers()
            .put(
                id,
                POIMarker.builder()
                    .label(
                        anchorBlock.name
                            + "\n"
                            + anchorBlock.blockPos.x()
                            + ", "
                            + anchorBlock.blockPos.z())
                    .position(
                        Vector3d.from(
                            anchorBlock.blockPos.x() + 0.5,
                            anchorBlock.blockPos.y() + 0.5,
                            anchorBlock.blockPos.z() + 0.5))
                    .icon("assets/" + MARKER_FILENAME, new Vector2i(25, 45))
                    .build());
      }

      for (Map.Entry<World, MarkerSet> entry : this.markerSets.entrySet()) {
        String mapId = "gw-" + entry.getKey().getName();
        this.api
            .getWorld(entry.getKey())
            .ifPresent(
                (BlueMapWorld world) -> {
                  for (BlueMapMap map : world.getMaps()) {
                    if (entry.getValue().getMarkers().isEmpty()) {
                      map.getMarkerSets().remove(mapId);
                    } else {
                      map.getMarkerSets().put(mapId, entry.getValue());
                    }
                  }
                });
      }
    }
  }

  private static final class NearbyPlayer {
    private final Player player;
    private final boolean isBedrock;

    private NearbyPlayer(Player player) {
      this.player = player;
      this.isBedrock = Players.isBedrock(player);
    }

    public Player getPlayer() {
      return this.player;
    }

    private boolean isJava() {
      return !this.isBedrock;
    }

    private boolean isBedrock() {
      return this.isBedrock;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || obj.getClass() != this.getClass()) {
        return false;
      }
      return this.player.equals(((NearbyPlayer) obj).player);
    }

    @Override
    public int hashCode() {
      return this.player.hashCode();
    }

    @Override
    public String toString() {
      return "NearbyPlayer[" + "player=" + this.player + ", " + "isBedrock=" + this.isBedrock + ']';
    }
  }

  private record BlockPos(World world, int x, int y, int z) {
    // same as Location, but for the block
    // can be replaced by io.papermc.paper.math.BlockPosition once that's no longer experimental
    @Override
    public String toString() {
      return "BlockPos{"
          + this.world.getName()
          + " "
          + this.x
          + ", "
          + this.y
          + ", "
          + this.z
          + '}';
    }

    private String getString(boolean includeWorld) {
      String prefix = "";
      if (includeWorld) {
        prefix =
            switch (this.world.getEnvironment()) {
              case NORMAL -> "Overworld ";
              case NETHER -> "Nether ";
              case THE_END -> "The End ";
              default -> "";
            };
      }
      return prefix + this.x + ", " + this.y + ", " + this.z;
    }

    @Override
    public boolean equals(@Nullable Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof BlockPos(World world1, int x1, int y1, int z1))) {
        return false;
      }
      return this.x == x1 && this.y == y1 && this.z == z1 && this.world.equals(world1);
    }

    private static BlockPos of(Location loc) {
      return new BlockPos(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private Location asLocation() {
      return new Location(this.world, this.x + 0.5, this.y, this.z + 0.5);
    }

    private Gateways.BlockPos down() {
      return new BlockPos(this.world, this.x, this.y - 1, this.z);
    }

    private Gateways.BlockPos up() {
      return new BlockPos(this.world, this.x, this.y + 1, this.z);
    }

    private Block getBlock() {
      return this.world.getBlockAt(this.x, this.y, this.z);
    }
  }
}
