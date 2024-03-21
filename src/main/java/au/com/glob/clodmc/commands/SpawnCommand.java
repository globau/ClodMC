package au.com.glob.clodmc.commands;

import au.com.glob.clodmc.BaseCommand;
import au.com.glob.clodmc.config.PlayerConfig;
import au.com.glob.clodmc.config.PluginConfig;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpawnCommand extends BaseCommand {
  @Override
  protected void execute(
      @NotNull Player player, @NotNull PlayerConfig playerConfig, @NotNull String[] args) {
    playerConfig.setHome(PlayerConfig.BACK_NAME, player.getLocation());
    World world = Bukkit.getWorld(PluginConfig.getInstance().getOverworldName());
    if (world == null) {
      return;
    }

    player.sendRichMessage("<grey>Teleporting you to spawn</grey>");

    // find random location around spawn point
    // governed by the SpawnRadius gamerule
    Integer spawnRadius = world.getGameRuleValue(GameRule.SPAWN_RADIUS);
    Random rand = new Random();
    double angle = rand.nextDouble() * 2 * Math.PI;
    double distance = rand.nextDouble() * (spawnRadius == null ? 10 : spawnRadius);
    Location loc = world.getSpawnLocation();
    loc.add(Math.round(distance + Math.cos(angle)), 0, Math.round(distance + Math.sin(angle)));

    // ensure the block isn't solid
    while (loc.getBlock().isCollidable()) {
      loc.add(0, 1, 0);
    }

    // teleport to the center of the block, just above the surface as per vanilla
    player.teleportAsync(loc.add(0.5, 0.1, 0.5));
  }
}
