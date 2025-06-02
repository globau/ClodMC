package au.com.glob.clodmc.datafile;

import au.com.glob.clodmc.util.Logger;
import au.com.glob.clodmc.util.TimeUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DataFile extends YamlConfiguration {
  private static final @NotNull DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final @NotNull File file;
  private boolean exists;

  public DataFile(@NotNull File file) {
    super();

    this.file = file;
    try {
      this.load(this.file);
      this.exists = true;
    } catch (FileNotFoundException ignore) {
      // ignore
    } catch (IOException e) {
      Logger.error("failed to load " + this.file + ": " + e);
    } catch (InvalidConfigurationException e) {
      Logger.error("malformed " + this.file + ": " + e);
    }
  }

  public boolean isNewFile() {
    return !this.exists;
  }

  public void save() {
    try {
      this.save(this.file);
    } catch (IOException e) {
      Logger.error(this.file + ": save failed: " + e);
    }
  }

  public void remove(@NotNull String path) {
    this.set(path, null);
  }

  public void setDateTime(@NotNull String path, @NotNull LocalDateTime dateTime) {
    this.set(path, dateTime.format(DATE_TIME_FORMATTER));
  }

  @Nullable public LocalDateTime getDateTime(@NotNull String path) {
    try {
      return LocalDateTime.parse(this.getString(path, ""), DATE_TIME_FORMATTER);
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  public void touch(@NotNull String path) {
    this.setDateTime(path, TimeUtil.localNow());
  }
}
