package au.com.glob.clodmc.modules.invite;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.command.BaseCommand;
import au.com.glob.clodmc.command.CommandError;
import au.com.glob.clodmc.config.PluginConfig;
import au.com.glob.clodmc.util.HttpClient;
import au.com.glob.clodmc.util.Mailer;
import com.google.gson.JsonObject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class InviteCommand extends BaseCommand {
  public InviteCommand() {
    super();
    PluginConfig.getInstance().setDefaultValue("invite", "playtime-minutes", 60 * 4);
  }

  @Override
  protected void execute(@NotNull CommandSender sender, @NotNull String[] args)
      throws CommandError {
    // validate args
    if (args.length != 2 || (!args[0].equals("java") && !args[0].equals("bedrock"))) {
      throw new CommandError("usage: invite java|bedrock {name}");
    }
    boolean isJava = args[0].equals("java");
    String name = args[1].toLowerCase(Locale.ENGLISH);

    // check playtime
    if (sender instanceof Player player && !player.isOp()) {
      int ticks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
      long minutesPlayed = Math.round(ticks / 20.0 / 60.0);
      if (minutesPlayed < PluginConfig.getInstance().getInteger("invite", "playtime-minutes")) {
        throw new CommandError("You have not played long enough on this server to invite others");
      }
    }

    // ensure player isn't already whitelisted
    for (OfflinePlayer offlinePlayer : Bukkit.getServer().getWhitelistedPlayers()) {
      String offlineName = offlinePlayer.getName();
      if (offlineName == null) {
        continue;
      }
      if (offlineName.toLowerCase(Locale.ENGLISH).equals(name.toLowerCase(Locale.ENGLISH))) {
        throw new CommandError(name + " is already whitelisted");
      }
    }

    CompletableFuture<String> cf = new CompletableFuture<>();

    if (isJava) {
      Bukkit.getScheduler()
          .runTaskAsynchronously(
              ClodMC.getInstance(),
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
                    response.has("errorMessage") ? null : response.get("name").getAsString());
              });
    } else {
      Bukkit.getScheduler()
          .runTaskAsynchronously(
              ClodMC.getInstance(),
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
                          ClodMC.getInstance(),
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
            }.runTask(ClodMC.getInstance()));
  }
}
