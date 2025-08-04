package au.com.glob.clodmc.command;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** command executor with sender and two string arguments */
@FunctionalInterface
@NullMarked
public interface ExecutorESS extends Executor {
  void accept(EitherCommandSender sender, @Nullable String arg1, @Nullable String arg2)
      throws CommandError;
}
