package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.CommandError;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.SimpleCommand;
import au.com.glob.clodmc.util.Chat;
import au.com.glob.clodmc.util.HttpClient;
import au.com.glob.clodmc.util.Logger;
import au.com.glob.clodmc.util.Mailer;
import au.com.glob.clodmc.util.PlayerDataFile;
import com.google.gson.JsonObject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

/** Allows players with enough playtime on the server to add others to the whitelist */
public class InviteCommand extends SimpleCommand implements Module {
  private static final int PLAY_TIME = 240; // minutes

  public InviteCommand() {
    super("invite", "/invite <java | bedrock> <player>", "Teleport to previous location");
  }

  @Override
  protected void execute(@NotNull CommandSender sender, @NotNull List<String> args) {
    String type = this.popArg(args);
    String name = this.popArg(args).toLowerCase();
    if (!(type.equals("java") || type.equals("bedrock")) || name.isBlank()) {
      throw new CommandError(this.usageMessage);
    }

    boolean isJava = type.equals("java");

    // check playtime
    if (sender instanceof Player player && !player.isOp()) {
      int ticks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
      long minutesPlayed = Math.round(ticks / 20.0 / 60.0);
      if (minutesPlayed < PLAY_TIME) {
        throw new CommandError("You have not played long enough on this server to invite others");
      }
    }

    // ensure player isn't already whitelisted
    // checking player files is easier than checking the actual whitelist for bedrock
    for (UUID uuid : PlayerDataFile.knownUUIDs()) {
      PlayerDataFile config = PlayerDataFile.of(uuid);
      if (config.getPlayerName().equalsIgnoreCase(name)) {
        throw new CommandError(name + " is already whitelisted");
      }
    }

    CompletableFuture<String> cf = new CompletableFuture<>();

    if (isJava) {
      Bukkit.getScheduler()
          .runTaskAsynchronously(
              ClodMC.instance,
              () -> {
                String url =
                    "https://api.mojang.com/users/profiles/Xminecraft/"
                        + URLEncoder.encode(name, StandardCharsets.UTF_8);
                HttpClient.JsonHttpResponse result = HttpClient.getJSON(url);

                JsonObject response = result.getResponse();
                if (response == null) {
                  Chat.error(sender, "Player lookup failed");
                  Logger.warning(url + " failed: null response");
                  return;
                }

                cf.complete(
                    response.has("errorMessage") ? null : response.get("name").getAsString());
              });
    } else {
      Bukkit.getScheduler()
          .runTaskAsynchronously(
              ClodMC.instance,
              () -> {
                String url =
                    "https://api.geysermc.org/v2/xbox/xuid/"
                        + URLEncoder.encode(name, StandardCharsets.UTF_8);
                HttpClient.JsonHttpResponse result = HttpClient.getJSON(url);

                JsonObject response = result.getResponse();
                if (response == null) {
                  Chat.error(sender, "Player lookup failed");
                  Logger.warning(url + " failed: null response");
                  return;
                }

                cf.complete(response.has("xuid") ? name : null);
              });
    }

    cf.whenComplete(
        (String normalisedName, Throwable error) ->
            new BukkitRunnable() {
              @Override
              public void run() {
                if (error != null) {
                  Chat.error(sender, error.toString());
                  return;
                }
                if (normalisedName == null) {
                  Chat.error(
                      sender,
                      name
                          + " is not a valid "
                          + (isJava ? "Java" : "Bedrock")
                          + " player name</red>");
                  if (!isJava) {
                    Chat.info(
                        sender, "Something went wrong inviting " + name + ", contact an admin");
                  }
                  return;
                }

                // add to whitelist
                boolean success =
                    Bukkit.getServer()
                        .dispatchCommand(
                            Bukkit.getServer().getConsoleSender(),
                            (isJava ? "whitelist" : "fwhitelist") + " add " + name);

                if (success) {
                  Chat.info(sender, normalisedName + " added to the whitelist");
                  Bukkit.getScheduler()
                      .runTaskAsynchronously(
                          ClodMC.instance,
                          () -> {
                            try {
                              Mailer.emailAdmin(
                                  "clod-mc: "
                                      + normalisedName
                                      + " ("
                                      + (isJava ? "Java" : "Bedrock")
                                      + ")"
                                      + " added to the whitelist by "
                                      + sender.getName());
                            } catch (Mailer.MailerError e) {
                              Logger.warning(e.getMessage());
                            }
                          });
                }
              }
            }.runTask(ClodMC.instance));
  }

  @Override
  public @NotNull List<String> tabComplete(
      @NotNull CommandSender sender, @NotNull String alias, String @NotNull [] args)
      throws IllegalArgumentException {
    List<String> types = List.of("java", "bedrock");
    if (args.length == 0) {
      return types;
    } else if (args.length == 1) {
      return types.stream().filter((String value) -> value.startsWith(args[0])).toList();
    } else {
      return List.of();
    }
  }
}
