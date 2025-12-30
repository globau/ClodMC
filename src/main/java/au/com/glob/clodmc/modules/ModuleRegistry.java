package au.com.glob.clodmc.modules;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.command.CommandBuilder;
import au.com.glob.clodmc.events.PlayerTargetBlockListener;
import au.com.glob.clodmc.modules.bluemap.BlueMap;
import au.com.glob.clodmc.modules.crafting.ChorusFlower;
import au.com.glob.clodmc.modules.crafting.SporeBlossom;
import au.com.glob.clodmc.modules.interactions.FastLeafDecay;
import au.com.glob.clodmc.modules.interactions.NamedStorage;
import au.com.glob.clodmc.modules.interactions.SignedContainers;
import au.com.glob.clodmc.modules.interactions.VeinMiner;
import au.com.glob.clodmc.modules.interactions.WaxedItemFrames;
import au.com.glob.clodmc.modules.interactions.WaxedPressurePlates;
import au.com.glob.clodmc.modules.interactions.gateways.Gateways;
import au.com.glob.clodmc.modules.inventory.AdminInv;
import au.com.glob.clodmc.modules.inventory.InventoryRestore;
import au.com.glob.clodmc.modules.inventory.deeppockets.DeepPockets;
import au.com.glob.clodmc.modules.inventory.inventorysort.InventorySort;
import au.com.glob.clodmc.modules.mobs.BetterDrops;
import au.com.glob.clodmc.modules.mobs.ExplodingCreepers;
import au.com.glob.clodmc.modules.mobs.PreventMobGriefing;
import au.com.glob.clodmc.modules.mobs.SilenceMobs;
import au.com.glob.clodmc.modules.mobs.preventmobspawn.PreventMobSpawn;
import au.com.glob.clodmc.modules.player.Back;
import au.com.glob.clodmc.modules.player.DeathLog;
import au.com.glob.clodmc.modules.player.GameMode;
import au.com.glob.clodmc.modules.player.Homes;
import au.com.glob.clodmc.modules.player.Invite;
import au.com.glob.clodmc.modules.player.OpAlerts;
import au.com.glob.clodmc.modules.player.PlayerTracker;
import au.com.glob.clodmc.modules.player.Seen;
import au.com.glob.clodmc.modules.player.Sleep;
import au.com.glob.clodmc.modules.player.Spawn;
import au.com.glob.clodmc.modules.player.WelcomeBook;
import au.com.glob.clodmc.modules.player.afk.AFK;
import au.com.glob.clodmc.modules.player.offlinemessages.OfflineMessages;
import au.com.glob.clodmc.modules.server.ClodServerLinks;
import au.com.glob.clodmc.modules.server.MOTD;
import au.com.glob.clodmc.modules.server.RequiredPlugins;
import au.com.glob.clodmc.modules.server.ServerStatus;
import au.com.glob.clodmc.modules.server.heapmap.HeatMap;
import au.com.glob.clodmc.util.Logger;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** registry for all plugin modules with automatic registration and lifecycle management */
@SuppressWarnings({"UnstableApiUsage"})
@NullMarked
public class ModuleRegistry implements Iterable<Module>, PluginBootstrap {
  private final Map<Class<? extends Module>, Module> modules = new HashMap<>();

  // paper bootstrap lifecycle hook for early module registration
  @Override
  public void bootstrap(final BootstrapContext context) {
    VeinMiner.bootstrap(new BootstrapContextHelper(context));
  }

  // register all modules organised by category
  public void registerAll() {
    // custom event listeners
    registerListener(new PlayerTargetBlockListener());

    // core - used by other modules
    this.register(OpAlerts.class);

    // crafting
    this.register(ChorusFlower.class);
    this.register(SporeBlossom.class);

    // interactions
    this.register(FastLeafDecay.class);
    this.register(Gateways.class);
    this.register(NamedStorage.class);
    this.register(SignedContainers.class);
    this.register(VeinMiner.class, VeinMiner.REQUIRED_PLUGIN);
    this.register(WaxedItemFrames.class);
    this.register(WaxedPressurePlates.class);

    // inventory
    this.register(AdminInv.class);
    this.register(DeepPockets.class);
    this.register(InventoryRestore.class);
    this.register(InventorySort.class);

    // mobs
    this.register(BetterDrops.class);
    this.register(ExplodingCreepers.class);
    this.register(PreventMobGriefing.class);
    this.register(PreventMobSpawn.class, "GriefPrevention");
    this.register(SilenceMobs.class);

    // player
    this.register(AFK.class);
    this.register(Back.class);
    this.register(DeathLog.class);
    this.register(GameMode.class);
    this.register(Homes.class);
    this.register(Invite.class);
    this.register(OfflineMessages.class);
    this.register(PlayerTracker.class);
    this.register(Seen.class);
    this.register(Sleep.class);
    this.register(Spawn.class);
    this.register(WelcomeBook.class);

    // server
    this.register(ClodServerLinks.class);
    this.register(HeatMap.class);
    this.register(MOTD.class);
    this.register(RequiredPlugins.class);
    this.register(ServerStatus.class);

    // bluemap
    this.register(BlueMap.class, "BlueMap");

    // register commands built by modules
    CommandBuilder.registerBuilders();
  }

  // register individual module with plugin dependency validation
  private void register(
      final Class<? extends Module> moduleClass, final String... requiredPlugins) {
    for (final String plugin : requiredPlugins) {
      if (!Bukkit.getPluginManager().isPluginEnabled(plugin)) {
        Logger.warning(
            "Cannot load module %s: depends on plugin %s which is not enabled"
                .formatted(moduleClass.getSimpleName(), plugin));
        return;
      }
    }

    final Module module;
    try {
      module = moduleClass.getDeclaredConstructor().newInstance();
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
    this.modules.put(moduleClass, module);

    if (module instanceof final Listener listener) {
      registerListener(listener);
    }
  }

  private static void registerListener(final Listener listener) {
    Bukkit.getServer().getPluginManager().registerEvents(listener, ClodMC.instance);
  }

  // iterate over registered modules
  @Override
  public Iterator<Module> iterator() {
    return this.modules.values().iterator();
  }

  // get specific module instance by class type
  @SuppressWarnings("unchecked")
  public @Nullable <T extends Module> T get(final Class<T> moduleClass) {
    final Module module = this.modules.get(moduleClass);
    return (T) module;
  }
}
