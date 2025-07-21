package au.com.glob.clodmc.command;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
@NullMarked
public interface ExecutorEP extends Executor {
  void accept(EitherCommandSender sender, @Nullable Player arg1) throws CommandError;
}
