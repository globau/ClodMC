package au.com.glob.clodmc.modules.server;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.annotations.Audience;
import au.com.glob.clodmc.annotations.Doc;
import au.com.glob.clodmc.datafile.PlayerDataFile;
import au.com.glob.clodmc.datafile.PlayerDataFiles;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Logger;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.jspecify.annotations.NullMarked;

/*
To apply permissions to a player, edit their player datafile.
  eg. plugins/ClodMC/players/8a56e8f2-0bc0-42aa-8f20-7b9188d1efbc.yml

Add a top-level "permissions" field with a list of the permissions to apply:

permissions:
- coreprotect.*

The player will need to re-log to apply changes.
 */

@Doc(
    audience = Audience.SERVER,
    title = "Permissions",
    description = "Minimal permissions provider")
@NullMarked
public class Permissions extends Module implements Listener {
  private final HashMap<UUID, PermissionAttachment> playerAttachments = new HashMap<>();

  @EventHandler
  public void onPlayerJoin(final PlayerJoinEvent event) {
    final Player player = event.getPlayer();
    final PlayerDataFile dataFile = PlayerDataFiles.of(player);
    final List<String> permissions = dataFile.getStringList("permissions");
    if (permissions.isEmpty()) {
      return;
    }

    final PermissionAttachment attachment = player.addAttachment(ClodMC.instance);
    this.playerAttachments.put(player.getUniqueId(), attachment);
    Logger.info(
        "applying permissions to %s: %s"
            .formatted(player.getName(), String.join(" ", permissions)));
    for (final String permission : permissions) {
      attachment.setPermission(permission, true);
    }
  }

  @EventHandler
  public void onPlayerQuit(final PlayerQuitEvent event) {
    final Player player = event.getPlayer();
    final PermissionAttachment attachment = this.playerAttachments.remove(player.getUniqueId());
    if (attachment != null) {
      player.removeAttachment(attachment);
    }
  }
}
