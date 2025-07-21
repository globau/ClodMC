package au.com.glob.clodmc.modules.inventory;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.command.CommandBuilder;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Chat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Swap between player and admin inventories */
@NullMarked
public class AdminInv implements Module, Listener {
  private final Map<UUID, @Nullable ItemStack[]> playerInventories = new HashMap<>();
  private final Map<UUID, @Nullable ItemStack[]> adminInventories = new HashMap<>();

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

  private void storeInventory(Player player) {
    UUID uuid = player.getUniqueId();

    this.playerInventories.put(uuid, player.getInventory().getContents());

    if (this.adminInventories.containsKey(uuid)) {
      player.getInventory().setContents(this.adminInventories.get(uuid));
    } else {
      player.getInventory().clear();
    }
  }

  private void restoreInventory(Player player) {
    UUID uuid = player.getUniqueId();

    if (this.playerInventories.containsKey(uuid)) {
      this.adminInventories.put(uuid, player.getInventory().getContents());
      player.getInventory().setContents(this.playerInventories.remove(uuid));
    }
  }

  private boolean hasStoredInventory(Player player) {
    UUID uuid = player.getUniqueId();
    return this.playerInventories.containsKey(uuid);
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
