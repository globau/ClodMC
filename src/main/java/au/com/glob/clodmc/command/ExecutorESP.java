package au.com.glob.clodmc.command;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** command executor with sender, string argument, and player argument */
@FunctionalInterface
@NullMarked
public interface ExecutorESP extends Executor {
  void accept(EitherCommandSender sender, @Nullable String arg1, @Nullable Player arg2)
      throws CommandError;
}
