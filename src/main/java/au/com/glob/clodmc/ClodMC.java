package au.com.glob.clodmc;

import au.com.glob.clodmc.modules.homes.BackCommand;
import au.com.glob.clodmc.modules.homes.DelHomeCommand;
import au.com.glob.clodmc.modules.homes.HomeCommand;
import au.com.glob.clodmc.modules.homes.Homes;
import au.com.glob.clodmc.modules.homes.HomesCommand;
import au.com.glob.clodmc.modules.homes.SetHomeCommand;
import au.com.glob.clodmc.modules.homes.SpawnCommand;
import au.com.glob.clodmc.modules.invite.InviteCommand;
import au.com.glob.clodmc.modules.mobs.PreventMobGriefing;
import au.com.glob.clodmc.modules.mobs.PreventMobSpawn;
import au.com.glob.clodmc.modules.welcome.WelcomeCommand;
import au.com.glob.clodmc.modules.welcome.WelcomeGift;
import au.com.glob.clodmc.util.Mailer;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import java.io.File;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class ClodMC extends JavaPlugin {
  public static ClodMC instance;

  public ClodMC() {
    super();
    instance = this;
  }

  @Override
  public void onLoad() {
    File dataFolder = this.getDataFolder();
    if (!dataFolder.exists() && !dataFolder.mkdirs()) {
      logWarning("failed to create " + dataFolder);
    }

    CommandAPI.onLoad(new CommandAPIBukkitConfig(this).verboseOutput(true));
  }

  @Override
  public void onEnable() {
    CommandAPI.onEnable();

    Mailer.register();

    Homes.register();
    BackCommand.register();
    DelHomeCommand.register();
    HomeCommand.register();
    SpawnCommand.register();
    HomesCommand.register();
    SetHomeCommand.register();

    InviteCommand.register();

    PreventMobGriefing.register();
    PreventMobSpawn.register();

    WelcomeGift.register();
    WelcomeCommand.register();
  }

  @Override
  public void onDisable() {
    CommandAPI.onDisable();
  }

  public static void logWarning(@NotNull String message) {
    instance.getLogger().warning(message);
  }
}
