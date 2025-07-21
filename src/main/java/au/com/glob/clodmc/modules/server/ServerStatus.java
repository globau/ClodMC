package au.com.glob.clodmc.modules.server;

import au.com.glob.clodmc.command.CommandBuilder;
import au.com.glob.clodmc.command.EitherCommandSender;
import au.com.glob.clodmc.modules.Module;
import java.util.StringJoiner;
import org.bukkit.Bukkit;
import org.jspecify.annotations.NullMarked;

/** /tps for all players */
@NullMarked
public class ServerStatus implements Module {
  public ServerStatus() {
    CommandBuilder.build("server-status")
        .description("Shows server status")
        .executor(
            (EitherCommandSender sender) -> {
              double[] tps = Bukkit.getTPS();
              StringJoiner values = new StringJoiner(", ");
              for (double avg : tps) {
                String colour;
                if (avg > 18.0) {
                  colour = "green";
                } else if (avg > 16.0) {
                  colour = "yellow";
                } else {
                  colour = "red";
                }
                double value = Math.min(Math.round(avg * 100.0) / 100.0, 20.0);
                values.add("<" + colour + ">" + value + "</" + colour + ">");
              }
              sender.sendRichMessage("Ticks-per-second from last 1m, 5m, 15m: " + values);
              if (tps[0] > 18) {
                sender.sendRichMessage("<gold>Server is healthy");
              } else if (tps[0] > 16) {
                sender.sendRichMessage("<gold>Server is running slow");
              } else {
                sender.sendRichMessage("<gold>Server is running very slow");
              }
            });
  }
}
