package au.com.glob.clodmc.command;

import org.jspecify.annotations.NullMarked;

/** command executor that accepts any sender type */
@FunctionalInterface
@NullMarked
public interface ExecutorE extends Executor {
  void accept(EitherCommandSender sender) throws CommandError;
}
