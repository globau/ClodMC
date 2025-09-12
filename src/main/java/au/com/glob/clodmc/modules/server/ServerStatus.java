package au.com.glob.clodmc.modules.server;

import au.com.glob.clodmc.annotations.Audience;
import au.com.glob.clodmc.annotations.Doc;
import au.com.glob.clodmc.command.CommandBuilder;
import au.com.glob.clodmc.command.EitherCommandSender;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Chat;
import java.util.StringJoiner;
import org.bukkit.Bukkit;
import org.jspecify.annotations.NullMarked;

@Doc(
    audience = Audience.PLAYER,
    title = "Server Status",
    description = "Add simple server status/health command")
@NullMarked
public class ServerStatus implements Module {
  public ServerStatus() {
    CommandBuilder.build("server-status")
        .description("Shows server status")
        .executor(
            (final EitherCommandSender sender) -> {
              final double[] tps = Bukkit.getTPS();
              final StringJoiner values = new StringJoiner(", ");
              for (final double avg : tps) {
                final String colour;
                if (avg > 18.0) {
                  colour = "green";
                } else if (avg > 16.0) {
                  colour = "yellow";
                } else {
                  colour = "red";
                }
                final double value = Math.min(Math.round(avg * 100.0) / 100.0, 20.0);
                values.add("<%s>%s</%s>".formatted(colour, value, colour));
              }
              Chat.plain(sender, "Ticks-per-second from last 1m, 5m, 15m: %s".formatted(values));
              if (tps[0] > 18) {
                Chat.info(sender, "Server is healthy");
              } else if (tps[0] > 16) {
                Chat.info(sender, "Server is running slow");
              } else {
                Chat.info(sender, "Server is running very slow");
              }
            });
  }
}
