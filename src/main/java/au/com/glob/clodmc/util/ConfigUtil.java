package au.com.glob.clodmc.util;

import au.com.glob.clodmc.ClodMC;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Stream;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;
import org.yaml.snakeyaml.error.YAMLException;

/** config file helpers */
@NullMarked
public class ConfigUtil {
  public static boolean sanityChecked = false;

  public static class InvalidConfig extends Exception {
    private final List<String> errors;

    public InvalidConfig(List<String> errors) {
      this.errors = errors;
    }

    @Override
    public String getMessage() {
      return String.join("\n", this.errors);
    }

    public void logErrors() {
      for (String line : this.errors) {
        Logger.error(line);
      }
    }
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

  private static List<File> getConfigFiles() throws IOException {
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
