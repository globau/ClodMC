package au.com.glob.clodmc.command;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface ExecutorESP extends Executor {
  void accept(@NotNull EitherCommandSender sender, @Nullable String arg1, @Nullable Player arg2)
      throws CommandError;
}
