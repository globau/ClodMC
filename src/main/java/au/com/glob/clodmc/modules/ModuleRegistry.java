package au.com.glob.clodmc.modules;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.bluemap.BlueMap;
import au.com.glob.clodmc.modules.gateways.Gateways;
import au.com.glob.clodmc.modules.interactions.Decorations;
import au.com.glob.clodmc.modules.interactions.FastLeafDecay;
import au.com.glob.clodmc.modules.interactions.NamedStorage;
import au.com.glob.clodmc.modules.inventory.AdminInvCommand;
import au.com.glob.clodmc.modules.inventory.InventorySort;
import au.com.glob.clodmc.modules.mobs.BetterDrops;
import au.com.glob.clodmc.modules.mobs.PreventMobGriefing;
import au.com.glob.clodmc.modules.mobs.PreventMobSpawn;
import au.com.glob.clodmc.modules.player.AFK;
import au.com.glob.clodmc.modules.player.BackCommand;
import au.com.glob.clodmc.modules.player.GameModeCommand;
import au.com.glob.clodmc.modules.player.Homes;
import au.com.glob.clodmc.modules.player.InviteCommand;
import au.com.glob.clodmc.modules.player.OfflineMessages;
import au.com.glob.clodmc.modules.player.OpAlerts;
import au.com.glob.clodmc.modules.player.PlayerTracker;
import au.com.glob.clodmc.modules.player.SeenCommand;
import au.com.glob.clodmc.modules.player.Sleep;
import au.com.glob.clodmc.modules.player.SpawnCommand;
import au.com.glob.clodmc.modules.player.WelcomeBook;
import au.com.glob.clodmc.modules.server.CircularWorldBorder;
import au.com.glob.clodmc.modules.server.ClodServerLinks;
import au.com.glob.clodmc.modules.server.RequiredPlugins;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class ModuleRegistry {
  private final @NotNull Map<Class<? extends Module>, Module> modules = new HashMap<>();

  private void register(@NotNull Class<? extends Module> moduleClass) {
    Module module;
    try {
      module = moduleClass.getDeclaredConstructor().newInstance();
    } catch (InstantiationException
        | IllegalAccessException
        | InvocationTargetException
        | NoSuchMethodException e) {
      throw new RuntimeException(e);
    }

    String dependsOn = module.dependsOn();
    if (dependsOn != null && !Bukkit.getPluginManager().isPluginEnabled(dependsOn)) {
      ClodMC.logWarning(
          "Cannot load "
              + module.getClass().getSimpleName()
              + ": depends on plugin "
              + dependsOn
              + " which is not enabled");
      return;
    }

    this.modules.put(moduleClass, module);

    if (module instanceof Listener listener) {
      Bukkit.getServer().getPluginManager().registerEvents(listener, ClodMC.instance);
    }

    if (module instanceof SimpleCommand command) {
      Bukkit.getServer().getCommandMap().register("clod-mc", command);
    }

    for (SimpleCommand command : module.getCommands()) {
      Bukkit.getServer().getCommandMap().register("clod-mc", command);
    }
  }

  public void registerAll() {
    // core - used by other modules
    this.register(OpAlerts.class);

    // gateways
    this.register(Gateways.class);

    // interactions
    this.register(Decorations.class);
    this.register(FastLeafDecay.class);
    this.register(NamedStorage.class);

    // inventory
    this.register(AdminInvCommand.class);
    this.register(InventorySort.class);

    // mobs
    this.register(BetterDrops.class);
    this.register(PreventMobGriefing.class);
    this.register(PreventMobSpawn.class);

    // player
    this.register(AFK.class);
    this.register(BackCommand.class);
    this.register(GameModeCommand.class);
    this.register(Homes.class);
    this.register(InviteCommand.class);
    this.register(OfflineMessages.class);
    this.register(PlayerTracker.class);
    this.register(SeenCommand.class);
    this.register(Sleep.class);
    this.register(SpawnCommand.class);
    this.register(WelcomeBook.class);

    // server
    this.register(CircularWorldBorder.class);
    this.register(ClodServerLinks.class);
    this.register(RequiredPlugins.class);

    // bluemap
    this.register(BlueMap.class);
  }

  @SuppressWarnings("unchecked")
  public @NotNull <T extends Module> T get(@NotNull Class<T> moduleClass) {
    Module module = this.modules.get(moduleClass);
    assert module != null;
    return (T) module;
  }

  public @NotNull Collection<Module> all() {
    return this.modules.values();
  }
}
