package au.com.glob.clodmc.command;

import java.util.List;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface CompletorP extends Completor {
  @NotNull List<String> accept(@NotNull Player player, @NotNull List<String> args);
}
