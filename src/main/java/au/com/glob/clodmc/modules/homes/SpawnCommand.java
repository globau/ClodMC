package au.com.glob.clodmc.modules.homes;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.SimpleCommand;
import au.com.glob.clodmc.util.PlayerLocation;
import java.util.List;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpawnCommand extends SimpleCommand implements Module {
  public SpawnCommand() {
    super("spawn", "Teleport to spawn");
  }

  @Override
  protected void execute(@NotNull CommandSender sender, @NotNull List<String> args) {
    Player player = this.toPlayer(sender);

    String worldName = ClodMC.instance.getConfig().getString("homes.overworld-name", "world");
    World world = Bukkit.getWorld(worldName);
    if (world == null) {
      return;
    }

    Homes.instance.setBackLocation(player);

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
    PlayerLocation playerLoc = PlayerLocation.of(loc.add(0.5, 0.1, 0.5));
    playerLoc.teleportPlayer(player);
  }
}
