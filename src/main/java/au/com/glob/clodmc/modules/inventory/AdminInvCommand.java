package au.com.glob.clodmc.modules.inventory;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.CommandError;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.SimpleCommand;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class AdminInvCommand extends SimpleCommand implements Module, Listener {
  private final Map<UUID, ItemStack[]> playerInventories = new HashMap<>();
  private final Map<UUID, ItemStack[]> adminInventories = new HashMap<>();

  public AdminInvCommand() {
    super("admininv", "Toggle admin/player inventory");
  }

  private void storeInventory(@NotNull Player player) {
    UUID uuid = player.getUniqueId();

    this.playerInventories.put(uuid, player.getInventory().getContents());

    if (this.adminInventories.containsKey(uuid)) {
      player.getInventory().setContents(this.adminInventories.get(uuid));
    } else {
      player.getInventory().clear();
    }
  }

  private void restoreInventory(@NotNull Player player) {
    UUID uuid = player.getUniqueId();

    if (this.playerInventories.containsKey(uuid)) {
      this.adminInventories.put(uuid, player.getInventory().getContents());
      player.getInventory().setContents(this.playerInventories.remove(uuid));
    }
  }

  private boolean hasStoredInventory(@NotNull Player player) {
    UUID uuid = player.getUniqueId();
    return this.playerInventories.containsKey(uuid);
  }

  @Override
  protected void execute(@NotNull CommandSender sender, @NotNull List<String> args) {
    if (!(sender instanceof Player player)) {
      throw new CommandError("");
    }
    if (!player.isOp()) {
      throw new CommandError("You do not have permissions to run this command");
    }

    if (this.hasStoredInventory(player)) {
      this.restoreInventory(player);
      player.sendRichMessage("<yellow>Switched to Player inventory");
    } else {
      this.storeInventory(player);
      player.sendRichMessage("<yellow>Switched to Admin inventory");
    }
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    if (event.getPlayer().isOp()) {
      this.restoreInventory(event.getPlayer());
    }
  }

  @EventHandler
  public void onPluginDisable(PluginDisableEvent event) {
    if (event.getPlugin().equals(ClodMC.instance)) {
      for (UUID uuid : this.playerInventories.keySet()) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOp()) {
          this.restoreInventory(player);
        }
      }
    }
  }
}
