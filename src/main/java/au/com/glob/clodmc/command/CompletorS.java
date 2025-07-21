package au.com.glob.clodmc.command;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NullMarked;

@FunctionalInterface
@NullMarked
public interface CompletorS extends Completor {
  List<String> accept(CommandSender sender, List<String> args);
}
