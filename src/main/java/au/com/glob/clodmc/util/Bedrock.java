package au.com.glob.clodmc.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.geysermc.geyser.api.GeyserApi;
import org.jspecify.annotations.NullMarked;

/** utilities for detecting bedrock edition clients via geyser api */
@NullMarked
public class Bedrock {
  private static boolean geyserLoaded;

  static {
    try {
      GeyserApi.api();
      geyserLoaded = true;
    } catch (NoClassDefFoundError e) {
      geyserLoaded = false;
    }
  }

  // check if geyser plugin is available
  public static boolean isGeyserLoaded() {
    return geyserLoaded;
  }

  // not using a weak reference as the player count is low, and the server restarts nightly
  private static final Map<UUID, Boolean> IS_BEDROCK_CACHE = new HashMap<>();

  // check if a player uuid is from a bedrock client (cached)
  public static boolean isBedrockUUID(UUID uuid) {
    if (!IS_BEDROCK_CACHE.containsKey(uuid)) {
      IS_BEDROCK_CACHE.put(uuid, GeyserApi.api().connectionByUuid(uuid) != null);
    }
    return IS_BEDROCK_CACHE.getOrDefault(uuid, false);
  }
}
