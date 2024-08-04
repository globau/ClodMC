package au.com.glob.clodmc.command;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface ExecutorPS extends Executor {
  void accept(@NotNull Player player, @Nullable String arg1) throws CommandError;
}
