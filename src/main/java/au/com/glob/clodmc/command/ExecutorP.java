package au.com.glob.clodmc.command;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@FunctionalInterface
@NullMarked
public interface ExecutorP extends Executor {
  void accept(Player player) throws CommandError;
}
