package au.com.glob.clodmc.command;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** command executor with sender, player argument, and string argument */
@FunctionalInterface
@NullMarked
public interface ExecutorEPS extends Executor {
  void accept(EitherCommandSender sender, @Nullable Player arg1, @Nullable String arg2)
      throws CommandError;
}
