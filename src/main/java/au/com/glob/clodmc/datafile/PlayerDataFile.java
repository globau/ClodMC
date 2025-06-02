package au.com.glob.clodmc.datafile;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.util.ConfigUtil;
import java.io.File;
import java.time.LocalDateTime;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerDataFile extends DataFile {
  protected PlayerDataFile(@NotNull String filename) {
    super(new File(ClodMC.instance.getDataFolder(), filename));

    if (!ConfigUtil.sanityChecked) {
      Bukkit.shutdown();
      throw new RuntimeException("player config file loaded before sanity checks");
    }
  }

  public @NotNull String getPlayerName() {
    return this.getString("player.name", "");
  }

  public void setPlayerName(@NotNull String name) {
    this.set("player.name", name);
  }

  public @Nullable LocalDateTime getLastLogin() {
    return this.getDateTime("player.last_login");
  }

  public void touchLastLogin() {
    this.touch("player.last_login");
  }

  public @Nullable LocalDateTime getLastLogout() {
    return this.getDateTime("player.last_logout");
  }

  public void touchLastLogout() {
    this.touch("player.last_logout");
  }

  public long getPlaytimeMins() {
    return this.getLong("player.playtime_min", 0);
  }

  public void setPlaytimeMins(long value) {
    this.set("player.playtime_min", value);
  }

  public void setInvitedBy(@NotNull String name) {
    this.set("player.invited_by", name);
  }
}
