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
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Player built point-to-point teleports */
public class Gateways implements Module, Listener {
  @SuppressWarnings({"NotNullFieldNotInitialized", "NullAway.Init"})
  private static @NotNull Gateways instance;

  private static @Nullable BlueMapGateways blueMapGateways;

  private static final int MAX_RANDOM_TP_TIME = 60; // minutes
  private static final int MIN_RANDOM_TP_DISTANCE = 1500;
  private static final int RANDOM_TP_COOLDOWN = 60; // seconds

  private final @NotNull File configFile =
      new File(ClodMC.instance.getDataFolder(), "gateways.yml");
  private final @NotNull Map<BlockPos, AnchorBlock> instances = new HashMap<>();
  private final @NotNull Map<Player, BlockPos> ignore = new HashMap<>();
  private final @NotNull Random random = new Random();

  private static final @NotNull List<Colour> COLOURS =
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

    CommandBuilder.build(
        "gateways",
        (CommandBuilder builder) -> {
          builder.description("List gateway in use");
          builder.executor(
              (@NotNull EitherCommandSender sender) -> {
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
  public void onPrepareItemCraft(@NotNull PrepareItemCraftEvent event) {
    ItemStack item = event.getInventory().getResult();
    if (!AnchorItem.isAnchor(item)) {
      return;
    }

    ItemStack[] matrix = event.getInventory().getMatrix();
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
  public void onCraftItem(@NotNull CraftItemEvent event) {
    ItemStack item = event.getCurrentItem();

    if (AnchorItem.isAnchor(item)) {
      AnchorItem.clearExtraMeta(item);
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
    AnchorBlock otherAnchorBlock = anchorBlock.connectedAnchorBlock();
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
    anchorBlock.blockPos.getWorld().dropItem(anchorBlock.blockPos.asLocation(), anchorItem);
  }

  @EventHandler
  public void onPlayerMove(@NotNull PlayerMoveEvent event) {
    Player player = event.getPlayer();
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
      teleportPos =
          Objects.requireNonNull(anchorBlock.connectedAnchorBlock()).teleportLocation(player);

      // set cause to PLUGIN so this teleport is ignored by /back
      cause = PlayerTeleportEvent.TeleportCause.PLUGIN;
    }

    // teleport
    this.ignore.put(player, anchorBlock.blockPos.up());
    Location finalTeleportPos = teleportPos;
    player
        .teleportAsync(teleportPos, cause)
        .whenComplete(
            (Boolean result, Throwable e) -> {
              player.clearTitle();
              if (result != null && result) {
                this.ignore.put(player, BlockPos.of(finalTeleportPos));
                player.playSound(finalTeleportPos, Sound.ENTITY_PLAYER_TELEPORT, 1, 1);
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
  public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
    event.getPlayer().discoverRecipe(AnchorItem.RECIPE_KEY);

    // if player spawns on an anchor block don't immediately teleport
    Player player = event.getPlayer();
    BlockPos standingOnPos = BlockPos.of(player.getLocation()).down();
    AnchorBlock anchorBlock = this.instances.get(standingOnPos);
    if (anchorBlock != null && anchorBlock.isConnected()) {
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

  private static int coloursToNetworkId(@NotNull Colour topColour, @NotNull Colour bottomColour) {
    return (topColour.index << 4) | bottomColour.index;
  }

  private static @NotNull Network networkIdToColours(int networkId) {
    return new Network(networkId);
  }

  //

  @SerializableAs("ClodMC.AnchorBlock")
  public static class AnchorBlock implements ConfigurationSerializable {
    final int networkId;
    final @NotNull BlockPos blockPos;
    final @Nullable String name;
    final @NotNull String displayName;

    private final @NotNull Location topLocation;
    private final @NotNull Location bottomLocation;
    private final @NotNull Colour topColour;
    private final @NotNull Colour bottomColour;

    private @Nullable Gateways.AnchorBlock connectedTo = null;
    private @Nullable BukkitTask particleTask = null;
    final boolean isRandom;

    public AnchorBlock(int networkId, @NotNull Location location, @Nullable String name) {
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
    public @NotNull String toString() {
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

    public @NotNull BlockPos getBlockPos() {
      return this.blockPos;
    }

    public @Nullable String getName() {
      return this.name;
    }

    public @NotNull String getInformation() {
      String prefix = "<yellow>" + this.displayName + "</yellow> - ";
      if (this.isConnected()) {
        return prefix
            + this.connectedTo.blockPos.getString(
                !this.blockPos.world.equals(this.connectedTo.blockPos.world));
      }
      if (this.networkId == RANDOM_NETWORK_ID) {
        return prefix + "Random Location";
      }
      return prefix + "Disconnected";
    }

    public @NotNull Colour getTopColour() {
      return this.topColour;
    }

    public @NotNull Colour getBottomColour() {
      return this.bottomColour;
    }

    private @NotNull String getColourPair() {
      return this.topColour.getDisplayName() + " :: " + this.bottomColour.getDisplayName();
    }

    private void connectTo(@NotNull Gateways.AnchorBlock otherBlock) {
      this.connectedTo = otherBlock;
      otherBlock.connectedTo = this;
    }

    private void disconnect() {
      if (this.connectedTo != null) {
        this.connectedTo.connectedTo = null;
      }
      this.connectedTo = null;
    }

    private boolean isConnected() {
      return this.connectedTo != null;
    }

    private @Nullable Gateways.AnchorBlock connectedAnchorBlock() {
      return this.connectedTo;
    }

    private Location facingLocation(@NotNull Location location) {
      double yawRadians = Math.toRadians(location.getYaw());
      double facingX = location.getX() - Math.sin(yawRadians);
      double facingZ = location.getZ() + Math.cos(yawRadians);
      return new Location(location.getWorld(), facingX, location.getY(), facingZ);
    }

    private Block facingBlock(@NotNull Location location) {
      return this.facingLocation(location).getBlock();
    }

    private boolean isFacingAnchor(@NotNull Location location) {
      return Gateways.instance.instances.containsKey(BlockPos.of(this.facingLocation(location)));
    }

    private boolean isFacingAir(@NotNull Location location) {
      return this.facingBlock(location).isEmpty();
    }

    private boolean isFacingSolid(@NotNull Location location) {
      return this.facingBlock(location).isSolid();
    }

    private @NotNull Location teleportLocation(@NotNull Player player) {
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

      if (this.particleTask != null) {
        this.particleTask.cancel();
      }

      if (isActive) {
        this.particleTask =
            Schedule.periodically(
                20,
                () -> {
                  Collection<Player> players = this.getNearbyPlayers(12);
                  new ParticleBuilder(Particle.DUST)
                      .location(this.topLocation)
                      .data(new Particle.DustOptions(this.topColour.color, 2))
                      .receivers(players)
                      .count(5)
                      .spawn();
                  new ParticleBuilder(Particle.DUST)
                      .location(this.bottomLocation)
                      .data(new Particle.DustOptions(this.bottomColour.color, 2))
                      .receivers(players)
                      .count(5)
                      .spawn();
                });
      } else {
        this.particleTask =
            Schedule.periodically(
                20,
                () -> {
                  Collection<Player> players = this.getNearbyPlayers(8);
                  new ParticleBuilder(Particle.DUST)
                      .location(this.topLocation)
                      .receivers(players)
                      .data(new Particle.DustOptions(this.topColour.color, 1))
                      .count(1)
                      .spawn();
                  new ParticleBuilder(Particle.DUST)
                      .location(this.bottomLocation)
                      .receivers(players)
                      .data(new Particle.DustOptions(this.bottomColour.color, 1))
                      .count(1)
                      .spawn();
                });
      }
    }

    private void updateLights(int lightLevel) {
      World world = this.blockPos.getWorld();
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

    private @NotNull Collection<Player> getNearbyPlayers(int radius) {
      // nearby players, excluding those standing on the anchor
      return this.bottomLocation
          .getWorld()
          .getNearbyPlayers(this.bottomLocation, radius, radius, radius)
          .stream()
          .filter(
              (Player player) -> {
                Location playerLoc = player.getLocation();
                return !(playerLoc.getWorld().equals(this.bottomLocation.getWorld())
                    && playerLoc.getBlockX() == this.bottomLocation.getBlockX()
                    && playerLoc.getBlockY() == this.bottomLocation.getBlockY()
                    && playerLoc.getBlockZ() == this.bottomLocation.getBlockZ());
              })
          .toList();
    }

    private void stopVisuals() {
      if (this.particleTask != null) {
        this.particleTask.cancel();
        this.particleTask = null;
      }
      this.updateLights(0);
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
      // note: doesn't store adjustY
      Map<String, Object> serialised = new HashMap<>();
      serialised.put("title", this.displayName);
      serialised.put("world", this.blockPos.getWorld().getName());
      serialised.put("x", this.blockPos.getX());
      serialised.put("y", this.blockPos.getY());
      serialised.put("z", this.blockPos.getZ());
      serialised.put("id", this.networkId);
      if (this.name != null) {
        serialised.put("name", this.name);
      }
      return serialised;
    }

    @SuppressWarnings("unused")
    public static @NotNull Gateways.AnchorBlock deserialize(@NotNull Map<String, Object> args) {
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
    private static final @NotNull NamespacedKey RECIPE_KEY = new NamespacedKey("clod-mc", "anchor");
    private static final String @NotNull [] SHAPE = new String[] {"PWP", "EWE", "ERE"};
    private static final @NotNull Map<Character, Material> SHAPE_MATERIALS =
        Map.of(
            'P', Material.ENDER_PEARL,
            'W', Material.AIR,
            'E', Material.END_STONE,
            'R', Material.RESPAWN_ANCHOR);

    private static final @NotNull String DEFAULT_ANCHOR_NAME = "Gateway Anchor";

    private static final @NotNull NamespacedKey NETWORK_KEY =
        new NamespacedKey("clod-mc", "network");
    private static final @NotNull NamespacedKey TOP_KEY =
        new NamespacedKey("clod-mc", "network-top");
    private static final @NotNull NamespacedKey BOTTOM_KEY =
        new NamespacedKey("clod-mc", "network-bottom");

    private static @NotNull ShapedRecipe getRecipe() {
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

    private static @NotNull ItemStack create() {
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

    private static int getNetworkId(@NotNull ItemStack item) {
      Integer networkId =
          item.getItemMeta()
              .getPersistentDataContainer()
              .get(NETWORK_KEY, PersistentDataType.INTEGER);
      if (networkId == null) {
        throw new RuntimeException("invalid anchor item state: malformed or missing network-id");
      }
      return networkId;
    }

    private static @Nullable String getName(@NotNull ItemStack item) {
      Component displayName = item.getItemMeta().displayName();
      String plainTextName = StringUtil.asText(Objects.requireNonNull(displayName));
      return plainTextName.equals(DEFAULT_ANCHOR_NAME) ? null : plainTextName;
    }

    private static void setMeta(
        @NotNull ItemStack anchorItem,
        int networkId,
        @Nullable String name,
        @Nullable String suffix) {
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

    private static void clearExtraMeta(@NotNull ItemStack anchorItem) {
      ItemMeta meta = anchorItem.getItemMeta();
      Integer networkIdBoxed =
          meta.getPersistentDataContainer().get(NETWORK_KEY, PersistentDataType.INTEGER);
      setMeta(anchorItem, Objects.requireNonNull(networkIdBoxed), null, null);
    }
  }

  //

  public record Colour(
      @NotNull Material material, @NotNull String name, int index, @NotNull Color color) {
    @Override
    public @NotNull String toString() {
      return this.name;
    }

    public @NotNull String getName() {
      return this.name;
    }

    private @NotNull String getDisplayName() {
      return StringUtil.toTitleCase(this.name.replace('_', ' '));
    }

    private @NotNull TextComponent getText() {
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
    public final @NotNull Gateways.Colour top;
    public final @NotNull Gateways.Colour bottom;

    Network(int networkId) {
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
    private static final @NotNull String MARKER_FILENAME = "gateway.svg";

    private final @NotNull Map<World, MarkerSet> markerSets = new HashMap<>(3);

    public BlueMapGateways(@NotNull BlueMapAPI api) {
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
      if (this.api == null) {
        return;
      }

      for (MarkerSet markerSet : this.markerSets.values()) {
        markerSet.getMarkers().clear();
      }

      Set<String> seenColours = new HashSet<>();
      for (AnchorBlock anchorBlock : Gateways.instance.getAnchorBlocks()) {
        if (anchorBlock.getName() == null) {
          continue;
        }

        String id =
            "gw-"
                + anchorBlock.getTopColour().getName()
                + "-"
                + anchorBlock.getBottomColour().getName()
                + "-";
        id = id + (seenColours.contains(id) ? "b" : "a");
        seenColours.add(id);

        Objects.requireNonNull(this.markerSets.get(anchorBlock.getBlockPos().getWorld()))
            .getMarkers()
            .put(
                id,
                POIMarker.builder()
                    .label(
                        anchorBlock.getName()
                            + "\n"
                            + anchorBlock.getBlockPos().getX()
                            + ", "
                            + anchorBlock.getBlockPos().getZ())
                    .position(
                        Vector3d.from(
                            anchorBlock.getBlockPos().getX() + 0.5,
                            anchorBlock.getBlockPos().getY() + 0.5,
                            anchorBlock.getBlockPos().getZ() + 0.5))
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

  public static class BlockPos {
    // same as Location, but for the block
    // can be replaced by io.papermc.paper.math.BlockPosition once that's no longer experimental
    final @NotNull World world;
    final int x;
    final int y;
    final int z;

    private BlockPos(@NotNull World world, int x, int y, int z) {
      this.world = world;
      this.x = x;
      this.y = y;
      this.z = z;
    }

    @Override
    public @NotNull String toString() {
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

    public @NotNull String getString(boolean includeWorld) {
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
    public final boolean equals(@Nullable Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof BlockPos otherPos)) {
        return false;
      }
      return this.x == otherPos.x
          && this.y == otherPos.y
          && this.z == otherPos.z
          && this.world.equals(otherPos.world);
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.world, this.x, this.y, this.z);
    }

    public static BlockPos of(@NotNull Location loc) {
      return new BlockPos(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public @NotNull Location asLocation() {
      return new Location(this.world, this.x + 0.5, this.y, this.z + 0.5);
    }

    public @NotNull World getWorld() {
      return this.world;
    }

    public int getX() {
      return this.x;
    }

    public int getY() {
      return this.y;
    }

    public int getZ() {
      return this.z;
    }

    public @NotNull Gateways.BlockPos down() {
      return new BlockPos(this.world, this.x, this.y - 1, this.z);
    }

    public @NotNull Gateways.BlockPos up() {
      return new BlockPos(this.world, this.x, this.y + 1, this.z);
    }

    public @NotNull Block getBlock() {
      return this.world.getBlockAt(this.x, this.y, this.z);
    }
  }
}
