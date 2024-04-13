package au.com.glob.clodmc.modules.invite;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.util.HttpClient;
import au.com.glob.clodmc.util.Mailer;
import com.google.gson.JsonObject;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class InviteCommand {
  private static final int DEFAULT_PLAYTIME = 60 * 4;

  public static void register() {
    FileConfiguration config = ClodMC.instance.getConfig();
    if (!config.contains("invite.playtime-minutes")) {
      config.set("invite.playtime", config.get("invite.playtime", DEFAULT_PLAYTIME));
      ClodMC.instance.saveConfig();
    }

    new CommandAPICommand("invite")
        .withShortDescription("Allow a player to join the server")
        .withArguments(new MultiLiteralArgument("type", "java", "bedrock"))
        .withArguments(new StringArgument("player"))
        .executes(
            (CommandSender sender, CommandArguments args) -> {
              boolean isJava = Objects.equals(args.get("type"), "java");
              String name =
                  ((String) Objects.requireNonNull(args.get("player"))).toLowerCase(Locale.ENGLISH);

              // check playtime
              if (sender instanceof Player player && !player.isOp()) {
                int ticks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
                long minutesPlayed = Math.round(ticks / 20.0 / 60.0);
                int minPlaytime = (int) config.get("invite.playtime-minutes", DEFAULT_PLAYTIME);
                if (minutesPlayed < minPlaytime) {
                  throw CommandAPI.failWithString(
                      "You have not played long enough on this server to invite others");
                }
              }

              // ensure player isn't already whitelisted
              for (OfflinePlayer offlinePlayer : Bukkit.getServer().getWhitelistedPlayers()) {
                String offlineName = offlinePlayer.getName();
                if (offlineName == null) {
                  continue;
                }
                if (offlineName
                    .toLowerCase(Locale.ENGLISH)
                    .equals(name.toLowerCase(Locale.ENGLISH))) {
                  throw CommandAPI.failWithString(name + " is already whitelisted");
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
                            sender.sendRichMessage("<red>player lookup failed</red>");
                            ClodMC.logWarning(url + " failed: null response");
                            return;
                          }

                          cf.complete(
                              response.has("errorMessage")
                                  ? null
                                  : response.get("name").getAsString());
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
                            sender.sendRichMessage("<red>player lookup failed</red>");
                            ClodMC.logWarning(url + " failed: null response");
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
                            sender.sendRichMessage("<red>" + error + "</red>");
                            return;
                          }
                          if (normalisedName == null) {
                            sender.sendRichMessage(
                                "<red>"
                                    + name
                                    + " is not a valid "
                                    + (isJava ? "Java" : "Bedrock")
                                    + " player name</red>");
                            return;
                          }

                          // add to whitelist
                          boolean success =
                              Bukkit.getServer()
                                  .dispatchCommand(
                                      Bukkit.getServer().getConsoleSender(),
                                      (isJava ? "whitelist" : "fwhitelist") + " add " + name);

                          if (success) {
                            sender.sendRichMessage(
                                "<yellow>" + normalisedName + " added to the whitelist</yellow>");
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
                                        ClodMC.logWarning(e.getMessage());
                                      }
                                    });
                          }
                        }
                      }.runTask(ClodMC.instance));
            })
        .register();
  }
}
