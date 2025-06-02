package au.com.glob.clodmc.command;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface ExecutorEPS extends Executor {
  void accept(@NotNull EitherCommandSender sender, @Nullable Player arg1, @Nullable String arg2)
      throws CommandError;
}
