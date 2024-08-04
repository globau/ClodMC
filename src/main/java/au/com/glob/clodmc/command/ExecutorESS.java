package au.com.glob.clodmc.command;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface ExecutorESS extends Executor {
  void accept(@NotNull EitherCommandSender sender, @Nullable String arg1, @Nullable String arg2)
      throws CommandError;
}
