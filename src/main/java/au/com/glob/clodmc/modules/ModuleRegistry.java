package au.com.glob.clodmc.modules;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.bluemap.BlueMap;
import au.com.glob.clodmc.modules.crafting.SporeBlossom;
import au.com.glob.clodmc.modules.interactions.FastLeafDecay;
import au.com.glob.clodmc.modules.interactions.Gateways;
import au.com.glob.clodmc.modules.interactions.NamedStorage;
import au.com.glob.clodmc.modules.interactions.SignedContainers;
import au.com.glob.clodmc.modules.interactions.VeinMiner;
import au.com.glob.clodmc.modules.interactions.WaxedItemFrames;
import au.com.glob.clodmc.modules.interactions.WaxedPressurePlates;
import au.com.glob.clodmc.modules.inventory.AdminInv;
import au.com.glob.clodmc.modules.inventory.InventorySort;
import au.com.glob.clodmc.modules.mobs.BetterDrops;
import au.com.glob.clodmc.modules.mobs.ExplodingCreepers;
import au.com.glob.clodmc.modules.mobs.PreventMobGriefing;
import au.com.glob.clodmc.modules.mobs.PreventMobSpawn;
import au.com.glob.clodmc.modules.player.AFK;
import au.com.glob.clodmc.modules.player.Back;
import au.com.glob.clodmc.modules.player.GameMode;
import au.com.glob.clodmc.modules.player.Homes;
import au.com.glob.clodmc.modules.player.Invite;
import au.com.glob.clodmc.modules.player.OfflineMessages;
import au.com.glob.clodmc.modules.player.OpAlerts;
import au.com.glob.clodmc.modules.player.PlayerTracker;
import au.com.glob.clodmc.modules.player.Seen;
import au.com.glob.clodmc.modules.player.Sleep;
import au.com.glob.clodmc.modules.player.Spawn;
import au.com.glob.clodmc.modules.player.WelcomeBook;
import au.com.glob.clodmc.modules.server.CircularWorldBorder;
import au.com.glob.clodmc.modules.server.ClodServerLinks;
import au.com.glob.clodmc.modules.server.MOTD;
import au.com.glob.clodmc.modules.server.RequiredPlugins;
import au.com.glob.clodmc.util.Logger;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings({"UnstableApiUsage", "unused"})
public class ModuleRegistry implements Iterable<Module>, PluginBootstrap {
  private final @NotNull Map<Class<? extends Module>, Module> modules = new HashMap<>();

  @Override
  public void bootstrap(@NotNull BootstrapContext context) {
    VeinMiner.bootstrap(new BootstrapContextHelper(context));
  }

  public void registerAll() {
    // core - used by other modules
    this.register(new OpAlerts());

    // crafting
    this.register(new SporeBlossom());

    // interactions
    this.register(new FastLeafDecay());
    this.register(new Gateways());
    this.register(new NamedStorage());
    this.register(new SignedContainers());
    this.register(new VeinMiner());
    this.register(new WaxedItemFrames());
    this.register(new WaxedPressurePlates());

    // inventory
    this.register(new AdminInv());
    this.register(new InventorySort());

    // mobs
    this.register(new BetterDrops());
    this.register(new ExplodingCreepers());
    this.register(new PreventMobGriefing());
    this.register(new PreventMobSpawn());

    // player
    this.register(new AFK());
    this.register(new Back());
    this.register(new GameMode());
    this.register(new Homes());
    this.register(new Invite());
    this.register(new OfflineMessages());
    this.register(new PlayerTracker());
    this.register(new Seen());
    this.register(new Sleep());
    this.register(new Spawn());
    this.register(new WelcomeBook());

    // server
    this.register(new CircularWorldBorder());
    this.register(new ClodServerLinks());
    this.register(new MOTD());
    this.register(new RequiredPlugins());

    // bluemap
    this.register(new BlueMap());
  }

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
    module.initialise();

    if (module instanceof Listener listener) {
      Bukkit.getServer().getPluginManager().registerEvents(listener, ClodMC.instance);
    }
  }

  @Override
  public @NotNull Iterator<Module> iterator() {
    return this.modules.values().iterator();
  }

  @SuppressWarnings("unchecked")
  public @NotNull <T extends Module> T get(@NotNull Class<T> moduleClass) {
    Module module = this.modules.get(moduleClass);
    return Objects.requireNonNull((T) module);
  }
}
