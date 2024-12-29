package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.command.CommandBuilder;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.PlayerLocation;
import au.com.glob.clodmc.util.TeleportUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** Teleport to spawn */
public class Spawn implements Module {
  public Spawn() {
    CommandBuilder.build(
        "spawn",
        (CommandBuilder builder) -> {
          builder.description("Teleport to spawn");
          builder.executor(
              (@NotNull Player player) -> {
                World world = Bukkit.getWorld("world");
                if (world == null) {
                  return;
                }

                Integer spawnRadius = world.getGameRuleValue(GameRule.SPAWN_RADIUS);
                Location loc =
                    TeleportUtil.getRandomLoc(
                        world.getSpawnLocation(), spawnRadius == null ? 8 : spawnRadius);

                // teleport to the center of the block, just above the surface as per vanilla
                PlayerLocation playerLoc = PlayerLocation.of(loc.add(0.5, 0.1, 0.5));
                playerLoc.teleportPlayer(player, "to spawn");
              });
        });
  }
}
