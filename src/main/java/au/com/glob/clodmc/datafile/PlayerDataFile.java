package au.com.glob.clodmc.datafile;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.util.ConfigUtil;
import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * YAML-based player data persistence with automatic UUID-based file management.
 *
 * <p>Provides type-safe access to player data stored in individual YAML files under the players/
 * directory. Instances are obtained via PlayerDataFiles, which controls when files are saved.
 *
 * <p>Data is stored in a structured format with common player metadata under the "player.*" path
 * and module-specific data under module-named paths.
 */
@NullMarked
public class PlayerDataFile {
  private final DataFile dataFile;

  PlayerDataFile(final UUID uuid) {
    if (!ConfigUtil.sanityChecked) {
      Bukkit.shutdown();
      throw new RuntimeException("player config file loaded before sanity checks");
    }

    this.dataFile =
        new DataFile(new File(ClodMC.instance.getDataFolder(), "players/%s.yml".formatted(uuid)));
  }

  // write to disk; only PlayerDataFiles.update may save
  void save() {
    this.dataFile.save();
  }

  // check if this file existed when loaded
  public boolean isNewFile() {
    return this.dataFile.isNewFile();
  }

  public @Nullable Object get(final String path) {
    return this.dataFile.get(path);
  }

  public @Nullable String getString(final String path) {
    return this.dataFile.getString(path);
  }

  public List<String> getStringList(final String path) {
    return this.dataFile.getStringList(path);
  }

  public @Nullable List<?> getList(final String path) {
    return this.dataFile.getList(path);
  }

  public <T extends ConfigurationSerializable> @Nullable T getSerializable(
      final String path, final Class<T> clazz) {
    return this.dataFile.getSerializable(path, clazz, null);
  }

  public @Nullable ConfigurationSection getConfigurationSection(final String path) {
    return this.dataFile.getConfigurationSection(path);
  }

  public @Nullable LocalDateTime getDateTime(final String path) {
    return this.dataFile.getDateTime(path);
  }

  public void set(final String path, @Nullable final Object value) {
    this.dataFile.set(path, value);
  }

  public void setDateTime(final String path, final LocalDateTime dateTime) {
    this.dataFile.setDateTime(path, dateTime);
  }

  public void remove(final String path) {
    this.dataFile.remove(path);
  }

  // get associated player name
  public String getPlayerName() {
    return this.dataFile.getString("player.name", "");
  }

  // set player name in datafile
  public void setPlayerName(final String name) {
    this.dataFile.set("player.name", name);
  }

  // get last login timestamp
  public @Nullable LocalDateTime getLastLogin() {
    return this.dataFile.getDateTime("player.last_login");
  }

  // update last login to current time
  public void touchLastLogin() {
    this.dataFile.touch("player.last_login");
  }

  // get last logout timestamp
  public @Nullable LocalDateTime getLastLogout() {
    return this.dataFile.getDateTime("player.last_logout");
  }

  // update last logout to current time
  public void touchLastLogout() {
    this.dataFile.touch("player.last_logout");
  }

  // get total playtime in minutes
  public long getPlaytimeMins() {
    return this.dataFile.getLong("player.playtime_min", 0);
  }

  // set total playtime in minutes
  public void setPlaytimeMins(final long value) {
    this.dataFile.set("player.playtime_min", value);
  }

  // record who invited this player
  public void setInvitedBy(final String name) {
    this.dataFile.set("player.invited_by", name);
  }
}
