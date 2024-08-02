package au.com.glob.clodmc.modules;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.bluemap.BlueMap;
import au.com.glob.clodmc.modules.gateways.Gateways;
import au.com.glob.clodmc.modules.interactions.Decorations;
import au.com.glob.clodmc.modules.interactions.FastLeafDecay;
import au.com.glob.clodmc.modules.interactions.NamedStorage;
import au.com.glob.clodmc.modules.interactions.SignedContainers;
import au.com.glob.clodmc.modules.interactions.WaxedItemFrames;
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
import au.com.glob.clodmc.util.Logger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class ModuleRegistry {
  private final @NotNull Map<Class<? extends Module>, Module> modules = new HashMap<>();

  private void register(@NotNull Module module) {
    String dependsOn = module.dependsOn();
    if (dependsOn != null && !Bukkit.getPluginManager().isPluginEnabled(dependsOn)) {
      Logger.warning(
          "Cannot load "
              + module.getClass().getSimpleName()
              + ": depends on plugin "
              + dependsOn
              + " which is not enabled");
      return;
    }

    this.modules.put(module.getClass(), module);

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
    this.register(new OpAlerts());

    // gateways
    this.register(new Gateways());

    // interactions
    this.register(new Decorations());
    this.register(new FastLeafDecay());
    this.register(new NamedStorage());
    this.register(new SignedContainers());
    this.register(new WaxedItemFrames());

    // inventory
    this.register(new AdminInvCommand());
    this.register(new InventorySort());

    // mobs
    this.register(new BetterDrops());
    this.register(new PreventMobGriefing());
    this.register(new PreventMobSpawn());

    // player
    this.register(new AFK());
    this.register(new BackCommand());
    this.register(new GameModeCommand());
    this.register(new Homes());
    this.register(new InviteCommand());
    this.register(new OfflineMessages());
    this.register(new PlayerTracker());
    this.register(new SeenCommand());
    this.register(new Sleep());
    this.register(new SpawnCommand());
    this.register(new WelcomeBook());

    // server
    this.register(new CircularWorldBorder());
    this.register(new ClodServerLinks());
    this.register(new RequiredPlugins());

    // bluemap
    this.register(new BlueMap());
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
