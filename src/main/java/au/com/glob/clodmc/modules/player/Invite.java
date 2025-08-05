package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.command.CommandBuilder;
import au.com.glob.clodmc.command.CommandError;
import au.com.glob.clodmc.command.CommandUsageError;
import au.com.glob.clodmc.command.EitherCommandSender;
import au.com.glob.clodmc.datafile.PlayerDataFile;
import au.com.glob.clodmc.datafile.PlayerDataFiles;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Chat;
import au.com.glob.clodmc.util.ClientType;
import au.com.glob.clodmc.util.HttpClient;
import au.com.glob.clodmc.util.HttpJsonResponse;
import au.com.glob.clodmc.util.Logger;
import au.com.glob.clodmc.util.Mailer;
import au.com.glob.clodmc.util.Players;
import au.com.glob.clodmc.util.Schedule;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Allows players with enough playtime on the server to add others to the whitelist */
@NullMarked
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
      Logger.error("bad or missing %s".formatted(secretsFile));
      secretApiKey = null;
    }
    this.apiKey = secretApiKey;

    if (this.apiKey == null) {
      return;
    }

    CommandBuilder.build("invite")
        .usage("/invite <java|bedrock> <player>")
        .description("Add a player to the whitelist")
        .executor(
            (EitherCommandSender sender, @Nullable String type, @Nullable String name) -> {
              if (ClientType.of(type) == null || name == null) {
                throw new CommandUsageError();
              }

              // check playtime
              if (sender.isPlayer() && !sender.isOp()) {
                int ticks = sender.asPlayer().getStatistic(Statistic.PLAY_ONE_MINUTE);
                long minutesPlayed = Math.round(ticks / 20.0 / 60.0);
                if (minutesPlayed < MIN_PLAY_TIME) {
                  throw new CommandError(
                      "You have not played on Clod-MC long enough to invite others");
                }
              }

              // don't allow adding by uuid; this helps ensure we don't have player name conflicts
              // as we're running floodgate without a prefix
              try {
                UUID.fromString(name.toLowerCase(Locale.ENGLISH));
                throw new CommandError("Invalid player name");
              } catch (IllegalArgumentException e) {
                // ignore
              }

              Schedule.asynchronously(
                  () -> {
                    try {
                      ClientType clientType = ClientType.of(type);

                      // check mcprofile.io
                      UUID uuid = this.lookupUUID(Objects.requireNonNull(clientType), name);
                      if (uuid == null) {
                        throw new CommandError(
                            "Failed to find player with name: %s".formatted(name));
                      }

                      // check whitelist by uuid
                      if (Players.isInWhitelistConfig(uuid)) {
                        throw new CommandError("%s is already whitelisted".formatted(name));
                      }

                      // don't allow duplicate player names (because we run without a
                      // floodgate prefix).
                      if (Players.isWhitelisted(name)) {
                        throw new CommandError("A player named %s already exists".formatted(name));
                      }

                      // add to appropriate whitelist
                      Schedule.nextTick(
                          () -> {
                            try {
                              String command =
                                  (clientType == ClientType.JAVA
                                      ? "whitelist add %s".formatted(name)
                                      : "fwhitelist add %s".formatted(uuid));
                              if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)) {
                                throw new CommandError("whitelist command failed");
                              }

                              // notify player
                              if (sender.isPlayer()) {
                                Chat.info(sender, "%s added to the whitelist".formatted(name));
                              }

                              // record who invited the new player
                              PlayerDataFile dataFile = PlayerDataFiles.of(uuid);
                              dataFile.setPlayerName(name);
                              dataFile.setInvitedBy(sender.getName());
                              dataFile.save();

                              // email admin
                              Mailer.emailAdmin(
                                  "clod-mc: %s (%s) added to the whitelist by %s"
                                      .formatted(name, clientType, sender.getName()));
                            } catch (CommandError e) {
                              Chat.error(
                                  sender,
                                  Objects.requireNonNullElse(
                                      e.getMessage(), "Failed to whitelist player"));
                            }
                          });
                    } catch (CommandError e) {
                      Chat.error(
                          sender,
                          Objects.requireNonNullElse(e.getMessage(), "Failed to whitelist player"));
                    }
                  });
            })
        .completor(
            (CommandSender sender, List<String> args) -> {
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
            });
  }

  // lookup player uuid from mcprofile.io api
  private @Nullable UUID lookupUUID(ClientType clientType, String name) {
    assert this.apiKey != null;
    String url =
        "https://mcprofile.io/api/v1/%s/%s"
            .formatted(
                clientType == ClientType.JAVA ? "java/username" : "bedrock/gamertag",
                URLEncoder.encode(name, StandardCharsets.UTF_8));

    HttpJsonResponse result = HttpClient.getJSON(url, Map.of("x-api-key", this.apiKey));
    JsonObject response = result.getResponse();
    if (response == null) {
      return null;
    }

    String field = clientType == ClientType.JAVA ? "uuid" : "floodgateuid";
    return response.has(field) ? UUID.fromString(response.get(field).getAsString()) : null;
  }
}
