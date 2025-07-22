package au.com.glob.clodmc.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.geysermc.geyser.api.GeyserApi;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class Bedrock {
  // not using a weak reference as the player count is low, and the server restarts nightly
  private static final Map<UUID, Boolean> IS_BEDROCK_CACHE = new HashMap<>();

  public static void apiTest() {
    GeyserApi.api();
  }

  public static boolean isBedrockUUID(UUID uuid) {
    if (!IS_BEDROCK_CACHE.containsKey(uuid)) {
      IS_BEDROCK_CACHE.put(uuid, GeyserApi.api().connectionByUuid(uuid) != null);
    }
    return IS_BEDROCK_CACHE.getOrDefault(uuid, false);
  }
}
