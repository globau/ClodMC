package au.com.glob.clodmc.util;

import au.com.glob.clodmc.ClodMC;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Stream;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.representer.Representer;

/** config file helpers */
public class ConfigUtil {
  public static boolean sanityChecked = false;

  public static class InvalidConfig extends Exception {
    private final @NotNull List<String> errors;

    public InvalidConfig(@NotNull List<String> errors) {
      this.errors = errors;
    }

    @Override
    public @NotNull String getMessage() {
      return String.join("\n", this.errors);
    }

    public void logErrors() {
      for (String line : this.errors) {
        Logger.error(line);
      }
    }
  }

  public static void migrateConfigs() throws InvalidConfig {
    DumperOptions dumperOptions = new DumperOptions();
    dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    LoaderOptions loaderOptions = new LoaderOptions();
    loaderOptions.setMaxAliasesForCollections(Integer.MAX_VALUE);
    loaderOptions.setCodePointLimit(Integer.MAX_VALUE);
    loaderOptions.setNestingDepthLimit(100);
    Yaml yaml = new Yaml(new SafeConstructor(loaderOptions), new Representer(dumperOptions));
    try {
      for (File file : getConfigFiles()) {
        try {
          Map<String, Object> data = yaml.load(new FileInputStream(file));
          if (updateLocation(data)) {
            try (Writer writer = Files.newBufferedWriter(file.toPath())) {
              yaml.dump(data, writer);
            }
          }
        } catch (YAMLException | IOException e) {
          Logger.exception(e);
        }
      }
    } catch (IOException e) {
      Logger.exception(e);
    }
  }

  @SuppressWarnings("unchecked")
  private static boolean updateLocation(Map<String, Object> map) {
    boolean modifed = false;
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      Object value = entry.getValue();
      if (value instanceof Map) {
        boolean m = updateLocation((Map<String, Object>) value);
        modifed = modifed || m;
      }
      if (entry.getKey().equals("==") && "ClodMC.Location".equals(entry.getValue())) {
        entry.setValue("org.bukkit.Location");
        modifed = true;
      }
    }
    return modifed;
  }

  public static void sanityCheckConfigs() throws InvalidConfig {
    List<String> errors = new ArrayList<>(0);
    try {
      for (File file : getConfigFiles()) {
        try {
          new YamlConfiguration().load(file);
        } catch (YAMLException | IOException | InvalidConfigurationException e) {
          StringJoiner message = new StringJoiner(": ");
          message.add(file.toString());
          message.add(e.getMessage());
          if (e.getCause() != null) {
            message.add(e.getCause().getMessage());
          }
          errors.add(message.toString());
        }
      }
    } catch (IOException e) {
      errors.add(e.getMessage());
    }
    if (!errors.isEmpty()) {
      throw new InvalidConfig(errors);
    }

    sanityChecked = true;
  }

  private static @NotNull List<File> getConfigFiles() throws IOException {
    List<Path> paths;
    try (Stream<Path> dataPaths = Files.walk(ClodMC.instance.getDataFolder().toPath())) {
      paths = new ArrayList<>(dataPaths.toList());
    }
    return paths.stream()
        .filter(Files::isRegularFile)
        .filter((Path path) -> path.toString().endsWith(".yml"))
        .map(Path::toFile)
        .sorted()
        .toList();
  }
}
