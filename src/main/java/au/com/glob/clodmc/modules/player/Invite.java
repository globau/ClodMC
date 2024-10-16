package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.command.CommandBuilder;
import au.com.glob.clodmc.command.CommandError;
import au.com.glob.clodmc.command.CommandUsageError;
import au.com.glob.clodmc.command.EitherCommandSender;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Chat;
import au.com.glob.clodmc.util.HttpClient;
import au.com.glob.clodmc.util.Logger;
import au.com.glob.clodmc.util.Mailer;
import au.com.glob.clodmc.util.PlayerDataFile;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Allows players with enough playtime on the server to add others to the whitelist */
public class Invite implements Module {
  private static final int MIN_PLAY_TIME = 240; // minutes

  private final @Nullable String apiKey;

  public Invite() {
    String secretApiKey;
    File secretsFile = new File(ClodMC.instance.getDataFolder(), "secrets.yml");
    YamlConfiguration config = new YamlConfiguration();
    try {
      config.load(secretsFile);
      secretApiKey = config.getString("mcprofile.api-key");
    } catch (IOException | InvalidConfigurationException e) {
      Logger.error("bad or missing " + secretsFile);
      secretApiKey = null;
    }
    this.apiKey = secretApiKey;

    if (this.apiKey == null) {
      return;
    }

    CommandBuilder.build("invite")
        .usage("/invite <java|bedrock> <player>")
        .description("Teleport to previous location")
        .executor(
            (@NotNull EitherCommandSender sender, @Nullable String type, @Nullable String name) -> {
              if (GameType.of(type) == null || name == null) {
                throw new CommandUsageError();
              }

              // check playtime
              if (sender.isPlayer() && !sender.isOp()) {
                int ticks = sender.asPlayer().getStatistic(Statistic.PLAY_ONE_MINUTE);
                long minutesPlayed = Math.round(ticks / 20.0 / 60.0);
                if (minutesPlayed < MIN_PLAY_TIME) {
                  throw new CommandError(
                      "You have not played on Clod long enough to invite others");
                }
              }

              // don't allow adding by uuid; this ensure we don't have player name conflicts
              // as we're running floodgate without a prefix
              if (isValidUUID(name.toLowerCase())) {
                throw new CommandError("Invalid player name");
              }

              runAsync(
                  sender,
                  () -> {
                    GameType gameType = GameType.of(type);
                    assert gameType != null;

                    // check mcprofile.io
                    UUID uuid = this.lookupUUID(gameType, name);
                    if (uuid == null) {
                      throw new CommandError("Failed to find player with name: " + name);
                    }

                    // check whitelist by uuid
                    if (this.isWhitelisted(uuid)) {
                      throw new CommandError(name + " is already whitelisted");
                    }

                    // don't allow duplicate player names (because we run without a floodgate
                    // prefix). checking playerdata as whitelist.json doesn't contain player names
                    // for floodgate users.
                    for (UUID existingUUID : PlayerDataFile.knownUUIDs()) {
                      PlayerDataFile playerConfig = PlayerDataFile.of(existingUUID);
                      if (playerConfig.getPlayerName().equalsIgnoreCase(name)) {
                        throw new CommandError("A player named " + name + " already exists");
                      }
                    }

                    // add to appropriate whitelist
                    runNextTick(
                        sender,
                        () -> {
                          String command =
                              (gameType == GameType.JAVA
                                  ? "whitelist add " + name
                                  : "fwhitelist add " + uuid);
                          if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)) {
                            throw new CommandError("whitelist command failed");
                          }

                          // notify player
                          if (sender.isPlayer()) {
                            Chat.info(sender, name + " added to the whitelist");
                          }

                          // email admin
                          Mailer.emailAdmin(
                              "clod-mc: "
                                  + name
                                  + " ("
                                  + gameType
                                  + ")"
                                  + " added to the whitelist by "
                                  + sender.getName());
                        });
                  });
            })
        .completor(
            (@NotNull CommandSender sender, @NotNull List<String> args) -> {
              List<String> types = List.of("java", "bedrock");
              if (args.isEmpty()) {
                return types;
              }
              if (args.size() == 1) {
                return types.stream()
                    .filter((String value) -> value.startsWith(args.getFirst()))
                    .toList();
              }
              return List.of();
            })
        .register();
  }

  private static void runAsync(@NotNull EitherCommandSender sender, @NotNull Runnable task) {
    Bukkit.getScheduler()
        .runTaskAsynchronously(
            ClodMC.instance,
            () -> {
              try {
                task.run();
              } catch (CommandError e) {
                Chat.error(sender, e.getMessage());
              }
            });
  }

  private static void runNextTick(@NotNull EitherCommandSender sender, @NotNull Runnable task) {
    new BukkitRunnable() {
      @Override
      public void run() {
        try {
          task.run();
        } catch (CommandError e) {
          Chat.error(sender, e.getMessage());
        }
      }
    }.runTask(ClodMC.instance);
  }

  private static boolean isValidUUID(@Nullable String value) {
    if (value == null) {
      return false;
    }
    try {
      UUID.fromString(value);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private @Nullable UUID lookupUUID(@NotNull GameType gameType, @NotNull String name) {
    assert this.apiKey != null;
    String url =
        "https://mcprofile.io/api/v1/"
            + (gameType == GameType.JAVA ? "java/username" : "bedrock/gamertag")
            + "/"
            + URLEncoder.encode(name, StandardCharsets.UTF_8);

    HttpClient.JsonHttpResponse result = HttpClient.getJSON(url, Map.of("x-api-key", this.apiKey));
    JsonObject response = result.getResponse();
    if (response == null) {
      return null;
    }

    String field = gameType == GameType.JAVA ? "uuid" : "floodgateuid";
    return response.has(field) ? UUID.fromString(response.get(field).getAsString()) : null;
  }

  private boolean isWhitelisted(@NotNull UUID uuid) {
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

  private enum GameType {
    JAVA("java"),
    BEDROCK("bedrock");

    private final @NotNull String name;

    GameType(@NotNull String name) {
      this.name = name;
    }

    @Override
    public @NotNull String toString() {
      return this.name;
    }

    public static @Nullable GameType of(@Nullable String name) {
      if (name == null) {
        return null;
      }
      for (GameType gameType : GameType.values()) {
        if (gameType.name.equalsIgnoreCase(name)) {
          return gameType;
        }
      }
      return null;
    }
  }
}
