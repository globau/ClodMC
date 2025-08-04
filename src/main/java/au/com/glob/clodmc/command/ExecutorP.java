package au.com.glob.clodmc.command;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

/** command executor for player-only commands */
@FunctionalInterface
@NullMarked
public interface ExecutorP extends Executor {
  void accept(Player player) throws CommandError;
}
