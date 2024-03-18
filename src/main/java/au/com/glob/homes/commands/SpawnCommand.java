package au.com.glob.homes.commands;

import au.com.glob.homes.BaseCommand;
import au.com.glob.homes.config.PlayerConfig;
import au.com.glob.homes.config.PluginConfig;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpawnCommand extends BaseCommand {
  @Override
  protected void execute(
      @NotNull Player player, @NotNull PlayerConfig playerConfig, @NotNull String[] args) {
    player.sendRichMessage("<grey>Teleporting you to spawn</grey>");
    playerConfig.setHome(PlayerConfig.BACK_NAME, player.getLocation());
    World world = Bukkit.getWorld(PluginConfig.getInstance().getOverworldName());
    if (world != null) {
      player.teleportAsync(world.getSpawnLocation().add(0.5, 0, 0.5));
    }
  }
}
