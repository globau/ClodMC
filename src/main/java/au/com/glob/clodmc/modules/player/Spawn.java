package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.annotations.Audience;
import au.com.glob.clodmc.annotations.Doc;
import au.com.glob.clodmc.command.CommandBuilder;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.TeleportUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@Doc(
    audience = Audience.PLAYER,
    title = "Spawn Teleport",
    description = "Adds /spawn to teleport to spawn")
@NullMarked
public class Spawn implements Module {
  public Spawn() {
    CommandBuilder.build("spawn")
        .description("Teleport to spawn")
        .executor(
            (final Player player) -> {
              final World world = Bukkit.getWorld("world");
              if (world == null) {
                return;
              }

              final Integer spawnRadius = world.getGameRuleValue(GameRules.RESPAWN_RADIUS);
              final Location location =
                  TeleportUtil.getRandomLoc(
                      world.getSpawnLocation(), spawnRadius == null ? 8 : spawnRadius);

              // teleport to the centre of the block, just above the surface as per vanilla
              location.add(0.5, 0.1, 0.5);
              TeleportUtil.teleport(player, location, "to spawn");
            });
  }
}
