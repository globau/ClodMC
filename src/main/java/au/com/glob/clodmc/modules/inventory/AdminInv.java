package au.com.glob.clodmc.modules.inventory;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.annotations.Audience;
import au.com.glob.clodmc.annotations.Doc;
import au.com.glob.clodmc.command.CommandBuilder;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Chat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@Doc(
    audience = Audience.ADMIN,
    title = "Admin Inventory",
    description = "Swap between player and admin inventories")
@NullMarked
public class AdminInv implements Module, Listener {
  private final Map<UUID, @Nullable ItemStack[]> playerInventories = new HashMap<>();
  private final Map<UUID, @Nullable ItemStack[]> adminInventories = new HashMap<>();

  // register the admininv command for inventory swapping
  public AdminInv() {
    CommandBuilder.build("admininv")
        .description("Toggle admin/player inventory")
        .requiresOp()
        .executor(
            (Player player) -> {
              if (this.hasStoredInventory(player)) {
                this.restoreInventory(player);
                Chat.info(player, "Switched to Player inventory");
              } else {
                this.storeInventory(player);
                Chat.info(player, "Switched to Admin inventory");
              }
            });
  }

  // store current player inventory and switch to admin inventory
  private void storeInventory(Player player) {
    UUID uuid = player.getUniqueId();

    this.playerInventories.put(uuid, player.getInventory().getContents());

    if (this.adminInventories.containsKey(uuid)) {
      player.getInventory().setContents(this.adminInventories.get(uuid));
    } else {
      player.getInventory().clear();
    }
  }

  // restore original player inventory from storage
  private void restoreInventory(Player player) {
    UUID uuid = player.getUniqueId();

    if (this.playerInventories.containsKey(uuid)) {
      this.adminInventories.put(uuid, player.getInventory().getContents());
      player.getInventory().setContents(this.playerInventories.remove(uuid));
    }
  }

  // check if player has a stored inventory
  private boolean hasStoredInventory(Player player) {
    UUID uuid = player.getUniqueId();
    return this.playerInventories.containsKey(uuid);
  }

  // restore inventory when player quits to prevent item loss
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerQuit(PlayerQuitEvent event) {
    if (event.getPlayer().isOp()) {
      this.restoreInventory(event.getPlayer());
    }
  }

  // restore all inventories on plugin disable (shutdown) to prevent item loss
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
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
