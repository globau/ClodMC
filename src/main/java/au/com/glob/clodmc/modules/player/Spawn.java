package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.command.CommandBuilder;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Chat;
import au.com.glob.clodmc.util.PlayerLocation;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** Teleport to spawn */
public class Spawn implements Module {
  public Spawn() {
    CommandBuilder.build("spawn")
        .description("Teleport to spawn")
        .executor(
            (@NotNull Player player) -> {
              World world = Bukkit.getWorld("world");
              if (world == null) {
                return;
              }

              Chat.fyi(player, "Teleporting you to spawn");

              // find random location around spawn point
              // governed by the SpawnRadius gamerule
              Integer spawnRadius = world.getGameRuleValue(GameRule.SPAWN_RADIUS);
              Random rand = new Random();
              double angle = rand.nextDouble() * 2 * Math.PI;
              double distance = rand.nextDouble() * (spawnRadius == null ? 10 : spawnRadius);
              Location loc = world.getSpawnLocation();
              loc.add(
                  Math.round(distance + Math.cos(angle)),
                  0,
                  Math.round(distance + Math.sin(angle)));

              // ensure the block isn't solid
              while (loc.getBlock().isCollidable()) {
                loc.add(0, 1, 0);
              }

              // teleport to the center of the block, just above the surface as per vanilla
              PlayerLocation playerLoc = PlayerLocation.of(loc.add(0.5, 0.1, 0.5));
              playerLoc.teleportPlayer(player);
            })
        .register();
  }
}
