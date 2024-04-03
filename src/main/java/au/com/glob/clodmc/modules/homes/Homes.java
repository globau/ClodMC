package au.com.glob.clodmc.modules.homes;

import au.com.glob.clodmc.command.CommandError;
import au.com.glob.clodmc.command.CommandUtil;
import au.com.glob.clodmc.config.PlayerConfig;
import au.com.glob.clodmc.config.PluginConfig;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Homes {
  public static void register() {
    PluginConfig.getInstance().setDefaultValue("homes", "max-allowed", 2);
    PluginConfig.getInstance().setDefaultValue("homes", "overworld-name", "world");
  }

  public static @NotNull Argument<String> homesArgument(@NotNull String name) {
    return new StringArgument(name)
        .replaceSuggestions(
            ArgumentSuggestions.strings(
                (info) -> {
                  if (info.sender() instanceof Player player) {
                    try {
                      PlayerConfig playerConfig = CommandUtil.getPlayerConfig(player);
                      return playerConfig.getHomeNames().toArray(new String[0]);
                    } catch (CommandError e) {
                      return new String[0];
                    }
                  } else {
                    return new String[0];
                  }
                }));
  }
}
