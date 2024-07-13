package au.com.glob.clodmc.config;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.util.PlayerLocation;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

public class Config extends YamlConfiguration {
  protected static final @NotNull Map<String, Config> instances = new HashMap<>();

  static {
    ConfigurationSerialization.registerClass(PlayerLocation.class);
  }

  private static final @NotNull DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private static boolean sanityChecked;

  private final @NotNull File file;
  private boolean exists;
  private long mtime;

  @NotNull public static Config of(@NotNull String filename) {
    if (instances.containsKey(filename)) {
      Config config = instances.get(filename);
      if (config.isUnmodified()) {
        return config;
      }
    }
    instances.put(filename, new Config(filename));
    return instances.get(filename);
  }

  public static void preload(@NotNull String filename) {
    of(filename);
  }

  public static void unload(@NotNull String filename) {
    instances.remove(filename);
  }

  //

  public static void sanityCheckConfigs() throws RuntimeException {
    // ensure all configs can be deserialised, halt server if not
    List<String> errors = new ArrayList<>(0);
    try (Stream<Path> paths = Files.walk(ClodMC.instance.getDataFolder().toPath())) {
      paths
          .filter(Files::isRegularFile)
          .filter((Path path) -> path.toString().endsWith(".yml"))
          .sorted()
          .map(Path::toFile)
          .forEach(
              (File file) -> {
                try {
                  migrate(file);
                  YamlConfiguration.loadConfiguration(file);
                } catch (YAMLException e) {
                  StringJoiner message = new StringJoiner(": ");
                  message.add(file.toString());
                  message.add(e.getMessage());
                  if (e.getCause() != null) {
                    message.add(e.getCause().getMessage());
                  }
                  errors.add(message.toString());
                }
              });
    } catch (IOException e) {
      errors.add(e.getMessage());
    }
    if (!errors.isEmpty()) {
      for (String error : errors) {
        ClodMC.logError(error);
      }
      throw new RuntimeException();
    }
    sanityChecked = true;
  }

  private static void migrate(@NotNull File file) throws YAMLException {
    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    Yaml yaml = new Yaml(options);
    try (InputStream inputStream = new FileInputStream(file)) {
      Map<String, Object> obj = yaml.load(inputStream);
      if (migrateObject(obj)) {
        ClodMC.logInfo("migrating " + file);
        try (FileWriter writer = new FileWriter(file)) {
          yaml.dump(obj, writer);
        }
      }
    } catch (IOException e) {
      throw new YAMLException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private static boolean migrateObject(Map<String, Object> obj) {
    boolean modified = false;
    for (Map.Entry<String, Object> entry : obj.entrySet()) {
      String name = entry.getKey();
      Object value = entry.getValue();
      if (name.equals("==")) {
        if (value.equals("au.com.glob.clodmc.modules.gateways.AnchorBlock")) {
          obj.put("==", "ClodMC.AnchorBlock");
          modified = true;
        } else if (value.equals("Location") || value.equals("org.bukkit.Location")) {
          obj.put("==", "ClodMC.Location");
          modified = true;
        }
      } else if (value instanceof LinkedHashMap<?, ?> mapValue) {
        if (migrateObject((Map<String, Object>) mapValue)) {
          modified = true;
        }
      } else if (value instanceof List<?>) {
        for (Object listObj : (List<?>) value) {
          if (listObj instanceof LinkedHashMap<?, ?> mapValue) {
            if (migrateObject((Map<String, Object>) mapValue)) {
              modified = true;
            }
          }
        }
      }
    }
    return modified;
  }

  //

  protected Config(@NotNull String filename) {
    super();

    if (!sanityChecked) {
      Bukkit.shutdown();
      throw new RuntimeException("config file loaded before sanity checks");
    }

    this.file = new File(ClodMC.instance.getDataFolder(), filename);
    if (!this.file.exists()) {
      try {
        this.createDefault();
      } catch (IOException e) {
        ClodMC.logError("failed to create " + this.file + ": " + e);
      }
    }

    this.mtime = 0;
    try {
      this.load(this.file);
      this.mergeDefaults();
      this.mtime = this.file.lastModified();
      this.exists = true;
    } catch (IOException e) {
      ClodMC.logError("failed to load " + this.file + ": " + e);
    } catch (InvalidConfigurationException e) {
      ClodMC.logError("malformed " + this.file + ": " + e);
    }
  }

  public boolean isUnmodified() {
    return !this.exists || this.mtime != this.file.lastModified();
  }

  public boolean fileExists() {
    return this.exists;
  }

  protected void save() {
    try {
      if (!this.file.getParentFile().exists() && !this.file.getParentFile().mkdirs()) {
        ClodMC.logError(this.file + ": mkdir failed");
      }
      this.save(this.file);
    } catch (IOException e) {
      ClodMC.logError(this.file + ": save failed: " + e);
    }
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
}
