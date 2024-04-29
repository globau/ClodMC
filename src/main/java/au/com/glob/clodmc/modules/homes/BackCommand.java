package au.com.glob.clodmc.modules.homes;

import au.com.glob.clodmc.command.CommandUtil;
import au.com.glob.clodmc.util.PlayerLocation;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BackCommand {
  public static void register() {
    new CommandAPICommand("back")
        .withShortDescription("Teleport to previous location")
        .withRequirement((sender) -> sender instanceof Player)
        .executes(
            (CommandSender sender, CommandArguments args) -> {
              Player player = CommandUtil.senderToPlayer(sender);
              FileConfiguration config = Homes.instance.getConfig(player);

              PlayerLocation location = (PlayerLocation) config.get("internal.back");
              if (location == null) {
                throw CommandAPI.failWithString("No previous location");
              }

              BackCommand.store(player, config);

              player.sendRichMessage("<grey>Teleporting you back</grey>");
              try {
                location.teleportPlayer(player);
              } catch (PlayerLocation.LocationError e) {
                throw CommandAPI.failWithString(e.getMessage());
              }
            })
        .register();
  }

  public static void store(@NotNull Player player) {
    store(player, Homes.instance.getConfig(player));
  }

  public static void store(@NotNull Player player, @NotNull FileConfiguration config) {
    PlayerLocation playerLocation = PlayerLocation.of(player);
    config.set("internal.back", playerLocation);
    Homes.instance.saveConfig(player, config);
  }
}
