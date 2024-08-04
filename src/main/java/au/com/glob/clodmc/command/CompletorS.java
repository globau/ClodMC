package au.com.glob.clodmc.command;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface CompletorS extends Completor {
  @NotNull List<String> accept(@NotNull CommandSender sender, @NotNull List<String> args);
}
