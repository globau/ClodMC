package au.com.glob.clodmc.modules.interactions;

import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.MiscUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;

public class NamedStorage implements Module, Listener {
  @EventHandler
  public void onPlayerMove(@NotNull PlayerMoveEvent event) {
    Player player = event.getPlayer();
    Block block = player.getTargetBlockExact(4);
    if (block == null) {
      return;
    }
    BlockState blockState = block.getState();

    if (blockState instanceof Container container) {
      Component name = container.customName();
      if (name != null) {
        player.sendActionBar(
            MiniMessage.miniMessage().deserialize("<yellow>" + MiscUtil.translate(name)));
      }
    }
  }
}
