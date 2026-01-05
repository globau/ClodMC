package au.com.glob.clodmc.modules.player.afk;

import au.com.glob.clodmc.util.Chat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/** tracks player afk state and last interaction time */
@NullMarked
final class PlayerState {
  Player player;
  long lastInteractionTime;
  boolean isAway;
  long afkTime;

  // create new player state tracking
  PlayerState(final Player player) {
    this.player = player;
    this.lastInteractionTime = System.currentTimeMillis() / 1000;
    this.isAway = false;
    this.afkTime = 0;
  }

  // update last interaction time and return from afk
  public void onAction() {
    this.lastInteractionTime = System.currentTimeMillis() / 1000;
    if (this.isAway) {
      this.setBack(true);
    }
  }

  // toggle afk status manually via command
  public void toggleAway() {
    if (this.isAway) {
      this.onAction();
    } else {
      this.setAway(true);
    }
  }

  // set player as afk
  public void setAway(final boolean announce) {
    this.isAway = true;
    this.afkTime = System.currentTimeMillis() / 1000;
    AFK.getAfkTeam().addEntry(this.player.getName());
    if (announce) {
      this.announce();
    }
  }

  // set player as no longer afk
  public void setBack(final boolean announce) {
    this.isAway = false;
    this.afkTime = 0;
    AFK.getAfkTeam().removeEntry(this.player.getName());
    if (announce) {
      this.announce();
    }
  }

  // announce afk status change to all players
  private void announce() {
    for (final Player player : Bukkit.getOnlinePlayers()) {
      if (player.equals(this.player)) {
        Chat.fyi(player, this.isAway ? "You are now AFK" : "You are no longer AFK");
      } else {
        Chat.fyi(
            player,
            this.isAway
                ? "%s is now AFK".formatted(this.player.getName())
                : "%s is no longer AFK".formatted(this.player.getName()));
      }
    }
  }
}
