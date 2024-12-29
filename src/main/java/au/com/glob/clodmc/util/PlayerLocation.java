package au.com.glob.clodmc.util;

import java.util.Map;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;

/** Same as location, but also tracks if the player is flying */
@SerializableAs("ClodMC.Location")
public class PlayerLocation implements ConfigurationSerializable {
  private final @NotNull Location location;
  private final boolean isFlying;

  private PlayerLocation(@NotNull Location location, boolean isFlying) {
    this.location = location;
    this.isFlying = isFlying;
  }

  public static @NotNull PlayerLocation of(@NotNull Player player) {
    return new PlayerLocation(player.getLocation(), player.isFlying());
  }

  public static @NotNull PlayerLocation of(@NotNull Location location) {
    return new PlayerLocation(location, false);
  }

  public void teleportPlayer(@NotNull Player player, @NotNull String reason) {
    Location currentLoc = player.getLocation();

    Location destinationLoc;
    if (player.getGameMode().equals(GameMode.CREATIVE)) {
      player.setFlying(this.isFlying);
      destinationLoc = this.location;
    } else {
      destinationLoc = TeleportUtil.getSafePos(this.location);
      destinationLoc.setYaw(this.location.getYaw());
      destinationLoc.setPitch(this.location.getPitch());
    }

    if (BlockPos.of(currentLoc).equals(BlockPos.of(destinationLoc))) {
      Chat.fyi(player, "Teleport not required");
      return;
    }

    String prefix = "Teleporting you ";
    if (player.isInsideVehicle()) {
      if (player.getVehicle() instanceof Horse) {
        prefix = "Dismounting your horse and teleporting you ";
      } else if (player.getVehicle() instanceof Boat) {
        prefix = "Dismounting your boat and teleporting you ";
      } else {
        prefix = "Dismounting and teleporting you ";
      }
    }
    Chat.fyi(player, prefix + reason);

    player
        .teleportAsync(destinationLoc, PlayerTeleportEvent.TeleportCause.COMMAND)
        .thenAccept(
            (Boolean result) -> {
              if (result) {
                TeleportUtil.teleportEffect(currentLoc);
              }
            })
        .exceptionally(
            (Throwable ex) -> {
              Chat.error(player, "Teleport failed");
              return null;
            });
  }

  @Override
  public @NotNull Map<String, Object> serialize() {
    Map<String, Object> serialised = this.location.serialize();
    if (this.isFlying) {
      serialised.put("isFlying", true);
    }
    return serialised;
  }

  @SuppressWarnings("unused")
  public static @NotNull PlayerLocation deserialize(@NotNull Map<String, Object> args) {
    return new PlayerLocation(
        Location.deserialize(args), (Boolean) args.getOrDefault("isFlying", false));
  }
}
