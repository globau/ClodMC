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
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.error.YAMLException;

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
        ClodMC.logError(line);
      }
    }
  }

  public static void sanityCheckConfigs() throws InvalidConfig {
    ConfigurationSerialization.registerClass(PlayerLocation.class);

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
              });
    } catch (IOException e) {
      errors.add(e.getMessage());
    }
    if (!errors.isEmpty()) {
      throw new InvalidConfig(errors);
    }

    sanityChecked = true;
  }
}
