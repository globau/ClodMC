package au.com.glob.clodmc.command;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NullMarked;

/** tab completion for commands accepting any command sender */
@FunctionalInterface
@NullMarked
public interface CompletorS extends Completor {
  List<String> accept(CommandSender sender, List<String> args);
}
