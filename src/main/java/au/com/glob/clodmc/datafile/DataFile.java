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

@NullMarked
public class DataFile extends YamlConfiguration {
  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final File file;
  private boolean exists;

  public DataFile(File file) {
    super();

    this.file = file;
    try {
      this.load(this.file);
      this.exists = true;
    } catch (FileNotFoundException ignore) {
      // ignore
    } catch (IOException e) {
      Logger.error("failed to load %s: %s".formatted(this.file, e));
    } catch (InvalidConfigurationException e) {
      Logger.error("malformed %s: %s".formatted(this.file, e));
    }
  }

  public boolean isNewFile() {
    return !this.exists;
  }

  public void save() {
    try {
      this.save(this.file);
    } catch (IOException e) {
      Logger.error("%s: save failed: %s".formatted(this.file, e));
    }
  }

  public void remove(String path) {
    this.set(path, null);
  }

  public void setDateTime(String path, LocalDateTime dateTime) {
    this.set(path, dateTime.format(DATE_TIME_FORMATTER));
  }

  @Nullable public LocalDateTime getDateTime(String path) {
    try {
      return LocalDateTime.parse(this.getString(path, ""), DATE_TIME_FORMATTER);
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  public void touch(String path) {
    this.setDateTime(path, TimeUtil.localNow());
  }
}
