package au.com.glob.clodmc.command;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ExecutorP extends Executor {
  void accept(@NotNull Player player) throws CommandError;
}
