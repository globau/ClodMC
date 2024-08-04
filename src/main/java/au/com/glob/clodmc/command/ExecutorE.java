package au.com.glob.clodmc.command;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ExecutorE extends Executor {
  void accept(@NotNull EitherCommandSender sender) throws CommandError;
}
