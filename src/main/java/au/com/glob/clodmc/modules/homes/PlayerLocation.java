package au.com.glob.clodmc.modules.homes;

import java.util.Map;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@SerializableAs("Location")
public class PlayerLocation implements ConfigurationSerializable {
  static {
    ConfigurationSerialization.registerClass(PlayerLocation.class, "Location");
  }

  private final Location location;
  private final boolean isFlying;

  private PlayerLocation(@NotNull Location location, boolean isFlying) {
    this.location = location;
    this.isFlying = isFlying;
  }

  public static @NotNull PlayerLocation of(@NotNull Player player) {
    return new PlayerLocation(player.getLocation(), player.isFlying());
  }

  public void teleportPlayer(@NotNull Player player) {
    player.setFlying(this.isFlying);
    player.teleportAsync(this.location);
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
