package au.com.glob.clodmc.modules;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Module {
  default @Nullable String dependsOn() {
    return null;
  }

  default void loadConfig() {}

  default @NotNull List<? extends SimpleCommand> getCommands() {
    return List.of();
  }
}
