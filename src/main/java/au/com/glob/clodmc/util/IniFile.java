package au.com.glob.clodmc.util;

import au.com.glob.clodmc.ClodMC;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IniFile {
  private static final Pattern SECTION_PATTERN = Pattern.compile("\\s*\\[([^]]*)]\\s*");
  private static final Pattern KEY_VALUE_PATTER = Pattern.compile("\\s*([^=]*)=(.*)");
  private static final Pattern COMMENT_LINE = Pattern.compile("^[;|#].*");

  private final @NotNull File iniFile;
  private final @Nullable String header;
  private final Map<String, Map<String, Object>> ini = new HashMap<>();

  public IniFile(@NotNull File file, @Nullable String header) {
    this.iniFile = file;
    this.header = header == null ? null : "# " + header;

    try {
      this.loadIni(this.iniFile);
    } catch (final IOException e) {
      // ignore errors
    }
  }

  public boolean exists() {
    return this.iniFile.exists();
  }

  public synchronized void save() {
    try {
      this.saveIni(this.iniFile);
    } catch (IOException e) {
      ClodMC.logWarning("Failed to save config: " + e.getMessage());
    }
  }

  private void loadIni(@NotNull File inputFile) throws IOException {
    try (FileInputStream inputStream = new FileInputStream(inputFile)) {
      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
        String section = "_NO_SECTION";
        String line;
        while ((line = reader.readLine()) != null) {
          final Matcher commentMatcher = COMMENT_LINE.matcher(line);
          if (commentMatcher.matches()) {
            continue;
          }
          final Matcher sectionMather = SECTION_PATTERN.matcher(line);
          if (sectionMather.matches()) {
            section = sectionMather.group(1).trim();
            continue;
          }
          final Matcher keyValueMatcher = KEY_VALUE_PATTER.matcher(line);
          if (keyValueMatcher.matches()) {
            final String key = keyValueMatcher.group(1).trim();
            final String value = keyValueMatcher.group(2).trim();
            this.ini.computeIfAbsent(section, s -> new HashMap<>()).put(key, value);
          }
        }
      }
    }
  }

  private void saveIni(@NotNull File outputFile) throws IOException {
    try (FileWriter fileWriter = new FileWriter(outputFile, StandardCharsets.UTF_8)) {
      try (BufferedWriter writer = new BufferedWriter(fileWriter)) {
        if (this.header != null) {
          writer.write(this.header);
          writer.newLine();
          writer.newLine();
        }
        List<Map.Entry<String, Map<String, Object>>> sections =
            new ArrayList<>(this.ini.entrySet());
        sections.sort(Map.Entry.comparingByKey());
        for (Map.Entry<String, Map<String, Object>> section : sections) {
          List<Map.Entry<String, Object>> items = new ArrayList<>(section.getValue().entrySet());
          if (items.isEmpty()) {
            continue;
          }
          writer.write("[" + section.getKey() + "]");
          writer.newLine();
          items.sort(Map.Entry.comparingByKey());
          for (Map.Entry<String, Object> item : items) {
            writer.write(item.getKey() + " = ");
            writer.write(item.getValue().toString());
            writer.newLine();
          }
          writer.newLine();
        }
      }
    }
  }

  private @NotNull String normaliseKey(@NotNull String name) {
    return URLEncoder.encode(
        name.replace(":", "_").toLowerCase(Locale.ENGLISH), StandardCharsets.UTF_8);
  }

  public @NotNull List<String> getKeys(@NotNull String section) {
    List<String> result =
        this.hasSection(section)
            ? new ArrayList<>(this.ini.get(section).keySet())
            : new ArrayList<>();
    result.sort(Comparator.naturalOrder());
    return result;
  }

  public boolean hasSection(@NotNull String section) {
    return this.ini.containsKey(section);
  }

  public boolean hasKey(@NotNull String section, @NotNull String name) {
    name = this.normaliseKey(name);
    return this.ini.containsKey(section) && this.ini.get(section).containsKey(name);
  }

  public void remove(@NotNull String section, @NotNull String name) {
    name = this.normaliseKey(name);
    if (this.hasKey(section, name)) {
      this.ini.get(section).remove(name);
      this.save();
    }
  }

  private @Nullable String getString(@NotNull String section, @NotNull String name) {
    name = this.normaliseKey(name);
    return this.hasKey(section, name) ? (String) this.ini.get(section).get(name) : null;
  }

  public @NotNull String getString(
      @NotNull String section, @NotNull String name, @NotNull String defaultValue) {
    name = this.normaliseKey(name);
    return this.hasKey(section, name) ? (String) this.ini.get(section).get(name) : defaultValue;
  }

  public void setString(@NotNull String section, @NotNull String name, @NotNull String value) {
    name = this.normaliseKey(name);
    if (value.equals(this.getString(section, name))) {
      return;
    }
    if (!this.ini.containsKey(section)) {
      this.ini.put(section, new HashMap<>());
    }
    this.ini.get(section).put(name, value);
    this.save();
  }

  public int getInteger(@NotNull String section, @NotNull String name, int defaultValue) {
    name = this.normaliseKey(name);
    if (!this.hasKey(section, name)) {
      return defaultValue;
    }
    try {
      return this.hasKey(section, name)
          ? Integer.parseInt((String) this.ini.get(section).get(name))
          : defaultValue;
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  public void setInteger(@NotNull String section, @NotNull String name, int value) {
    this.setString(section, name, Integer.toString(value));
  }

  public @Nullable Location getLocation(
      @NotNull String section, @NotNull String name, @Nullable Location defaultValue) {
    name = this.normaliseKey(name);
    if (!this.hasKey(section, name)) {
      return defaultValue;
    }
    try {
      return this.hasKey(section, name)
          ? this.stringToLocation((String) this.ini.get(section).get(name))
          : defaultValue;
    } catch (IllegalArgumentException e) {
      return defaultValue;
    }
  }

  public void setLocation(@NotNull String section, @NotNull String name, @NotNull Location value) {
    this.setString(section, name, this.locationToString(value));
  }

  private @NotNull Location stringToLocation(@NotNull String value)
      throws IllegalArgumentException {
    Map<String, Object> args = new HashMap<>();
    for (String pair : value.split("\\s+")) {
      String[] parts = pair.split(":", 2);
      if (parts.length != 2) {
        throw new IllegalArgumentException();
      }
      args.put(parts[0], parts[1]);
    }
    return new Location(
        Bukkit.getWorld((String) args.get("world")),
        NumberConversions.toDouble(args.get("x")),
        NumberConversions.toDouble(args.get("y")),
        NumberConversions.toDouble(args.get("z")),
        NumberConversions.toFloat(args.get("yaw")),
        NumberConversions.toFloat(args.get("pitch")));
  }

  private @NotNull String locationToString(@NotNull Location location) {
    StringJoiner result = new StringJoiner(" ");
    result.add("world:" + location.getWorld().getName());
    result.add("x:" + String.format("%.2f", location.getX()));
    result.add("y:" + String.format("%.2f", location.getY()));
    result.add("z:" + String.format("%.2f", location.getZ()));
    result.add("yaw:" + String.format("%.2f", location.getYaw()));
    result.add("pitch:" + String.format("%.2f", location.getPitch()));
    return result.toString();
  }
}
