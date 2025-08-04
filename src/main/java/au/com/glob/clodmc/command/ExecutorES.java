package au.com.glob.clodmc.command;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** command executor with sender and string argument */
@FunctionalInterface
@NullMarked
public interface ExecutorES extends Executor {
  void accept(EitherCommandSender sender, @Nullable String arg1) throws CommandError;
}
