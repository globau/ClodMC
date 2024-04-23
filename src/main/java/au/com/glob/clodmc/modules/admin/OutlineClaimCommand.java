package au.com.glob.clodmc.modules.admin;

import au.com.glob.clodmc.command.CommandUtil;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.ItemStackArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.util.BoundingBox;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class OutlineClaimCommand {
  // create barriers below ground; this is useful to prevent mobs
  // that spawn just outside of the admin claim from pathing in
  // the claim.

  public static void register() {
    new CommandAPICommand("admin-outline-claim")
        .withShortDescription("outline current admin claim with specified block")
        .withPermission(CommandPermission.OP)
        .withRequirement((sender) -> sender instanceof Player)
        .withArguments(new ItemStackArgument("block"))
        .executes(
            (CommandSender sender, CommandArguments args) -> {
              Player player = CommandUtil.senderToPlayer(sender);
              World world = player.getWorld();

              ItemStack itemStack = (ItemStack) args.get("block");
              if (itemStack == null) {
                throw CommandAPI.failWithString("invalid block");
              }
              Material material = itemStack.getType();

              Claim claim =
                  GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), true, null);
              if (claim == null) {
                throw CommandAPI.failWithString("not standing within a claim");
              }
              if (!claim.isAdminClaim()) {
                throw CommandAPI.failWithString("not standing within an admin claim");
              }
              BoundingBox box = new BoundingBox(claim);

              for (int x = box.getMinX(); x <= box.getMaxX(); x++) {
                fillColumn(world, x, box.getMinZ(), material);
              }
              for (int x = box.getMinX(); x <= box.getMaxX(); x++) {
                fillColumn(world, x, box.getMaxZ(), material);
              }

              for (int z = box.getMinZ(); z <= box.getMaxZ(); z++) {
                fillColumn(world, box.getMinX(), z, material);
              }

              for (int z = box.getMinZ(); z <= box.getMaxZ(); z++) {
                fillColumn(world, box.getMaxX(), z, material);
              }
            })
        .register();
  }

  private static void fillColumn(World world, int x, int z, Material material) {
    int y = world.getHighestBlockYAt(x, z);

    // find the top-most solid block
    while (y >= -64) {
      Block block = world.getBlockAt(x, y, z);
      if (!block.isEmpty() && !block.isLiquid()) {
        break;
      }
      y--;
    }

    // change all air/water blocks below
    while (y >= -64) {
      Block block = world.getBlockAt(x, y, z);
      if (block.isEmpty() || block.isLiquid()) {
        block.setType(material);
      }
      y--;
    }
  }
}
