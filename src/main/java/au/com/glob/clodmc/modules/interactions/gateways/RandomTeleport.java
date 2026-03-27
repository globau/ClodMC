package au.com.glob.clodmc.modules.interactions.gateways;

import au.com.glob.clodmc.datafile.PlayerDataFile;
import au.com.glob.clodmc.datafile.PlayerDataFiles;
import au.com.glob.clodmc.util.Chat;
import au.com.glob.clodmc.util.StringUtil;
import au.com.glob.clodmc.util.TeleportUtil;
import au.com.glob.clodmc.util.TimeUtil;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Random;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** random teleport logic */
@NullMarked
final class RandomTeleport {
  private RandomTeleport() {}

  // a black-black gateway teleports the player to a random location
  private static final int RANDOM_NETWORK_ID =
      Network.coloursToNetworkId(
          Objects.requireNonNull(Colours.of(Material.BLACK_WOOL)),
          Objects.requireNonNull(Colours.of(Material.BLACK_WOOL)));

  private static final int MAX_RANDOM_TP_TIME = 60; // minutes
  private static final int MIN_RANDOM_TP_DISTANCE = 1500;
  private static final int RANDOM_TP_COOLDOWN = 60; // seconds

  private static final Random random = new Random();

  static boolean isRandomNetworkId(final int networkId) {
    return networkId == RANDOM_NETWORK_ID;
  }

  // find safe random teleport location for new players
  @Nullable static Location location(final Player player) {
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
        Chat.warning(
            player,
            "You must wait another %s before teleporting again"
                .formatted(
                    StringUtil.plural2(RANDOM_TP_COOLDOWN - secondsSinceRandomTeleport, "second")));
        return null;
      }
    }

    // show a message; normally this is only visible if the destination
    // chunk is slow to load
    player.showTitle(
        Title.title(
            Component.text(""),
            Component.text("Teleporting"),
            Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(500))));

    final World world = Bukkit.getWorld("world");
    if (world == null) {
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
            MIN_RANDOM_TP_DISTANCE + (random.nextDouble() * (radius - MIN_RANDOM_TP_DISTANCE));
        final double angle = 2 * Math.PI * random.nextDouble();
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

      dataFile.setDateTime("tpr", now);
      dataFile.save();

      return teleportPos;
    }
    Chat.error(player, "Failed to find a safe location");
    return null;
  }
}
