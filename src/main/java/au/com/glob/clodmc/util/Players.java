package au.com.glob.clodmc.util;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.datafile.PlayerDataFile;
import au.com.glob.clodmc.datafile.PlayerDataFiles;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** utilities for player management and bedrock client detection */
@NullMarked
public class Players {
  // Attribute.BLOCK_INTERACTION_RANGE + 1
  public static final int INTERACTION_RANGE = 5;

  private static volatile Map<String, UUID> whitelisted = new HashMap<>();

  // get current whitelist mapping name to uuid
  public static Map<String, UUID> getWhitelisted() {
    return whitelisted;
  }

  // check if uuid is already in whitelist.json
  public static boolean isInWhitelistConfig(UUID uuid) {
    String uuidString = uuid.toString();
    try {
      Path whitelistFile =
          Path.of(
              Bukkit.getServer().getWorldContainer().getCanonicalFile().getAbsolutePath(),
              "whitelist.json");
      String jsonContent = Files.readString(whitelistFile);
      for (JsonElement jsonElement : JsonParser.parseString(jsonContent).getAsJsonArray()) {
        JsonObject entry = jsonElement.getAsJsonObject();
        if (uuidString.equalsIgnoreCase(entry.get("uuid").getAsString())) {
          return true;
        }
      }
    } catch (IOException | JsonSyntaxException e) {
      Logger.exception(e);
    }
    return false;
  }

  // refresh whitelist from player data files
  public static void updateWhitelisted() {
    Schedule.asynchronously(
        () -> {
          File playersPath = ClodMC.instance.getDataFolder().toPath().resolve("players").toFile();
          File[] ymlFiles = playersPath.listFiles((File file) -> file.getName().endsWith(".yml"));
          List<UUID> uuids =
              ymlFiles == null
                  ? List.of()
                  : Arrays.stream(ymlFiles)
                      .map((File file) -> file.getName().substring(0, file.getName().indexOf(".")))
                      .map(UUID::fromString)
                      .toList();

          Map<String, UUID> updatedWhitelist = new HashMap<>();
          for (UUID uuid : uuids) {
            PlayerDataFile config = PlayerDataFiles.of(uuid);
            updatedWhitelist.put(config.getPlayerName(), uuid);
          }
          whitelisted = Collections.unmodifiableMap(updatedWhitelist);
        });
  }

  // check if player name is whitelisted
  public static boolean isWhitelisted(String name) {
    return whitelisted.keySet().stream().anyMatch((String p) -> p.equalsIgnoreCase(name));
  }

  // get uuid for whitelisted player name
  public static @Nullable UUID getWhitelistedUUID(String name) {
    return whitelisted.entrySet().stream()
        .filter((Map.Entry<String, UUID> entry) -> entry.getKey().equalsIgnoreCase(name))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElse(null);
  }

  // check if player is using bedrock client
  public static boolean isBedrock(Player player) {
    return Bedrock.isGeyserLoaded() && Bedrock.isBedrockUUID(player.getUniqueId());
  }
}
