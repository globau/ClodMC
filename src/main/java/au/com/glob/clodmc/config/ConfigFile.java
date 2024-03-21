package au.com.glob.clodmc.config;

import au.com.glob.clodmc.ClodMC;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConfigFile {
  private final File file;
  private final String comment;
  private final Map<String, String> values = new HashMap<>();

  public ConfigFile(@NotNull File file, @NotNull String comment) {
    this.file = file;
    this.comment = comment;

    Properties properties = new Properties();
    try {
      properties.load(new FileInputStream(file));
    } catch (IOException e) {
      // ignore
    }
    properties.forEach((key, value) -> this.values.put(key.toString(), (String) value));
  }

  public boolean exists() {
    return this.file.exists();
  }

  public void save() {
    try {
      // default writer always include timestamp as a comment, which sucks
      try (FileWriter fileWriter = new FileWriter(this.file, StandardCharsets.UTF_8)) {
        try (BufferedWriter writer = new BufferedWriter(fileWriter)) {
          writer.write("# " + this.comment);
          writer.newLine();
          for (Map.Entry<String, String> value : this.entrySet()) {
            writer.write(value.getKey() + "=" + value.getValue());
            writer.newLine();
          }
        }
      }
    } catch (IOException e) {
      ClodMC.getInstance().logWarning(e.getMessage());
    }
  }

  public @NotNull List<Map.Entry<String, String>> entrySet() {
    List<Map.Entry<String, String>> values = new ArrayList<>(this.values.entrySet());
    values.sort(Map.Entry.comparingByKey());
    return values;
  }

  public void setDefault(@NotNull String key, @NotNull String value) {
    if (!this.values.containsKey(key)) {
      this.values.put(key, value);
    }
  }

  public void setDefault(@NotNull String key, int value) {
    this.setDefault(key, String.valueOf(value));
  }

  public void set(@NotNull String key, @Nullable String value) {
    if (value == null) {
      this.values.remove(key);
    } else {
      this.values.put(key, value);
    }
  }

  public void set(@NotNull String key, int value) {
    this.set(key, String.valueOf(value));
  }

  public @Nullable String get(@NotNull String key) {
    return this.values.get(key);
  }

  public @NotNull String get(@NotNull String key, @NotNull String defaultValue) {
    String result = this.values.get(key);
    return result == null ? defaultValue : result;
  }

  public int get(@NotNull String key, int defaultValue) {
    try {
      return Integer.parseInt(this.values.get(key));
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }
}
