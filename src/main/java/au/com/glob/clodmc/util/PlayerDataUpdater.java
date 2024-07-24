package au.com.glob.clodmc.util;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerDataUpdater implements AutoCloseable {
  private final @NotNull PlayerDataFile config;
  private boolean modified;

  private PlayerDataUpdater(@NotNull UUID uuid) {
    this.config = PlayerDataFile.of(uuid);
  }

  public static @NotNull PlayerDataUpdater of(@NotNull Player player) {
    return new PlayerDataUpdater(player.getUniqueId());
  }

  public static @NotNull PlayerDataUpdater of(@NotNull UUID uuid) {
    return new PlayerDataUpdater(uuid);
  }

  @Override
  public void close() {
    if (this.modified) {
      this.config.save();
    }
  }

  public boolean fileExists() {
    return this.config.fileExists();
  }

  public void set(@NotNull String path, @NotNull Object value) {
    this.config.set(path, value);
    this.modified = true;
  }

  public void set(@NotNull String path, @NotNull LocalDateTime value) {
    this.config.setDateTime(path, value);
    this.modified = true;
  }

  public void remove(@NotNull String path) {
    this.config.set(path, null);
    this.modified = true;
  }

  public @Nullable ConfigurationSection getConfigurationSection(@NotNull String path) {
    return this.config.getConfigurationSection(path);
  }

  public @Nullable List<?> getList(@NotNull String path) {
    return this.config.getList(path);
  }
}
