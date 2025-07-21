package au.com.glob.clodmc.command;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
@NullMarked
public interface ExecutorPS extends Executor {
  void accept(Player player, @Nullable String arg1) throws CommandError;
}
