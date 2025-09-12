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
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** yaml configuration file with datetime handling and automatic loading */
@NullMarked
public class DataFile extends YamlConfiguration {
  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final File file;
  private boolean exists;

  // load yaml file, sets .exists if the file could be loaded
  public DataFile(final File file) {
    super();

    this.file = file;
    try {
      this.load(this.file);
      this.exists = true;
    } catch (final FileNotFoundException ignore) {
      // ignore
    } catch (final IOException e) {
      Logger.error("failed to load %s".formatted(this.file), e);
    } catch (final InvalidConfigurationException e) {
      Logger.error("malformed %s".formatted(this.file), e);
    }
  }

  // check if this file existed when loaded
  public boolean isNewFile() {
    return !this.exists;
  }

  // write to disk
  public void save() {
    try {
      this.save(this.file);
    } catch (final IOException e) {
      Logger.error("failed to write %s".formatted(this.file), e);
    }
  }

  // remove a key (YamlConfiguration won't write null values)
  public void remove(final String path) {
    this.set(path, null);
  }

  // store datetime as formatted string
  public void setDateTime(final String path, final LocalDateTime dateTime) {
    this.set(path, dateTime.format(DATE_TIME_FORMATTER));
  }

  // parse datetime from string, null if invalid
  @Nullable public LocalDateTime getDateTime(final String path) {
    try {
      return LocalDateTime.parse(this.getString(path, ""), DATE_TIME_FORMATTER);
    } catch (final DateTimeParseException e) {
      return null;
    }
  }

  // set path to current timestamp
  public void touch(final String path) {
    this.setDateTime(path, TimeUtil.localNow());
  }
}
