package au.com.glob.clodmc.command;

import java.util.List;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/** tab completion for player-only commands */
@FunctionalInterface
@NullMarked
public interface CompletorP extends Completor {
  List<String> accept(Player player, List<String> args);
}
