package au.com.glob.clodmc.util;

import au.com.glob.clodmc.ClodMC;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class Config extends YamlConfiguration {
  private static final @NotNull Map<String, Config> instances = new HashMap<>();

  private final @NotNull File file;
  private long mtime;

  @NotNull public static Config getInstance(@NotNull String filename) {
    if (instances.containsKey(filename)) {
      Config config = instances.get(filename);
      if (config.mtime == config.file.lastModified()) {
        return config;
      }
    }
    instances.put(filename, new Config(filename));
    return instances.get(filename);
  }

  public static void init(@NotNull String filename) {
    getInstance(filename);
  }

  private Config(@NotNull String filename) {
    super();

    this.file = new File(ClodMC.instance.getDataFolder(), filename);

    if (!this.file.exists()) {
      try {
        this.createDefault();
      } catch (IOException e) {
        ClodMC.logError("failed to create " + this.file + ": " + e);
      }
    }

    this.reload();
  }

  private void reload() {
    this.mtime = 0;
    this.map.clear();

    try {
      this.load(this.file);
      this.mergeDefaults();
      this.mtime = this.file.lastModified();
    } catch (IOException e) {
      ClodMC.logError("failed to load " + this.file + ": " + e);
    } catch (InvalidConfigurationException e) {
      ClodMC.logError("malformed " + this.file + ": " + e);
    }
  }

  public long lastModified() {
    return this.mtime;
  }

  private void createDefault() throws IOException {
    try (InputStream inStream = ClodMC.instance.getResource(this.file.getName())) {
      if (inStream != null) {
        Files.copy(inStream, this.file.toPath());
      }
    }
  }

  private void mergeDefaults() throws IOException {
    try (InputStream inStream = ClodMC.instance.getResource(this.file.getName())) {
      if (inStream == null) {
        return;
      }
      YamlConfiguration defaults =
          YamlConfiguration.loadConfiguration(new InputStreamReader(inStream));

      boolean updated = false;
      for (String path : defaults.getKeys(true)) {
        if (!this.contains(path)) {
          ClodMC.logInfo(this.file.getName() + ": added new config setting: " + path);
          this.set(path, defaults.get(path));
          updated = true;
        }
      }

      if (updated) {
        this.save(this.file);
      }
    }
  }
}
