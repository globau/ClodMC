package au.com.glob.clodmc.modules.player.afk;

import au.com.glob.clodmc.util.Chat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
final class PlayerState {
  final Player player;
  long lastInteractionTime;
  boolean isAway;

  PlayerState(Player player) {
    this.player = player;
    this.lastInteractionTime = System.currentTimeMillis() / 1000;
    this.isAway = false;
  }

  public void onAction() {
    this.lastInteractionTime = System.currentTimeMillis() / 1000;
    if (this.isAway) {
      this.setBack(true);
    }
  }

  public void toggleAway() {
    if (this.isAway) {
      this.onAction();
    } else {
      this.setAway(true);
    }
  }

  public void setAway(boolean announce) {
    this.isAway = true;
    AFK.instance.getAfkTeam().addEntry(this.player.getName());
    if (announce) {
      this.announce();
    }
  }

  public void setBack(boolean announce) {
    this.isAway = false;
    AFK.instance.getAfkTeam().removeEntry(this.player.getName());
    if (announce) {
      this.announce();
    }
  }

  private void announce() {
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (player.equals(this.player)) {
        Chat.fyi(player, this.isAway ? "You are now AFK" : "You are no longer AFK");
      } else {
        Chat.fyi(
            player,
            this.isAway
                ? this.player.getName() + " is now AFK"
                : this.player.getName() + " is no longer AFK");
      }
    }
  }
}
