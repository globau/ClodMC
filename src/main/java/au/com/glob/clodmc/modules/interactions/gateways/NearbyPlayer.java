package au.com.glob.clodmc.modules.interactions.gateways;

import au.com.glob.clodmc.util.Players;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** represents a nearby player with platform detection for gateway interactions */
@NullMarked
final class NearbyPlayer {
  final Player player;
  final boolean isBedrock;

  NearbyPlayer(Player player) {
    this.player = player;
    this.isBedrock = Players.isBedrock(player);
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
    return "NearbyPlayer[player=%s, isBedrock=%s]".formatted(this.player, this.isBedrock);
  }
}
