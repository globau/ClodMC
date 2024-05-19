package au.com.glob.clodmc.util;

import java.util.Map;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@SerializableAs("Location")
public class PlayerLocation implements ConfigurationSerializable {
  private final Location location;
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

  public void teleportPlayer(@NotNull Player player) {
    if (player.isInsideVehicle()) {
      if (player.getVehicle() instanceof Horse) {
        player.sendRichMessage("<grey>Dismounting your horse</grey>");
      } else if (player.getVehicle() instanceof Boat) {
        player.sendRichMessage("<grey>Dismounting your boat</grey>");
      } else {
        player.sendRichMessage("<grey>Dismounting</grey>");
      }
    }

    Location loc;
    if (player.getGameMode().equals(GameMode.CREATIVE)) {
      player.setFlying(this.isFlying);
      loc = this.location;
    } else {
      loc = BlockPos.of(this.location).getSafePos().asLocation();
      loc.setYaw(this.location.getYaw());
      loc.setPitch(this.location.getPitch());
    }
    player.teleportAsync(loc);
  }

  @Override
  public @NotNull Map<String, Object> serialize() {
    Map<String, Object> serialised = this.location.serialize();
    serialised.put("isFlying", this.isFlying);
    return serialised;
  }

  @SuppressWarnings("unused")
  public static @NotNull PlayerLocation deserialize(@NotNull Map<String, Object> args) {
    return new PlayerLocation(Location.deserialize(args), (Boolean) args.get("isFlying"));
  }
}
