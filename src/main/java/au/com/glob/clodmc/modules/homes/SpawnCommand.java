package au.com.glob.clodmc.modules.homes;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.command.CommandUtil;
import au.com.glob.clodmc.util.BlockPos;
import au.com.glob.clodmc.util.PlayerLocation;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnCommand {
  public static void register() {
    new CommandAPICommand("spawn")
        .withShortDescription("Teleport to spawn")
        .withRequirement((sender) -> sender instanceof Player)
        .executes(
            (CommandSender sender, CommandArguments args) -> {
              Player player = CommandUtil.senderToPlayer(sender);

              String worldName =
                  (String) ClodMC.instance.getConfig().get("homes.overworld-name", "world");
              World world = Bukkit.getWorld(worldName);
              if (world == null) {
                return;
              }

              BackCommand.store(player);

              player.sendRichMessage("<grey>Teleporting you to spawn</grey>");

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
              try {
                PlayerLocation playerLoc = PlayerLocation.of(loc.add(0.5, 0.1, 0.5));
                playerLoc.teleportPlayer(player);
              } catch (BlockPos.LocationError e) {
                throw CommandAPI.failWithString(e.getMessage());
              }
            })
        .register();
  }
}
