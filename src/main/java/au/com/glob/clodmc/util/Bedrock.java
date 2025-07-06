package au.com.glob.clodmc.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.geysermc.geyser.api.GeyserApi;
import org.jetbrains.annotations.NotNull;

public class Bedrock {
  private static final @NotNull Map<UUID, Boolean> IS_BEDROCK_CACHE = new HashMap<>();

  public static void apiTest() {
    GeyserApi.api();
  }

  public static boolean isBedrockUUID(@NotNull UUID uuid) {
    Boolean result = IS_BEDROCK_CACHE.getOrDefault(uuid, null);
    if (result == null) {
      result = GeyserApi.api().connectionByUuid(uuid) != null;
      IS_BEDROCK_CACHE.put(uuid, result);
    }
    return result;
  }
}
