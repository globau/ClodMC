package au.com.glob.clodmc.modules;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.command.CommandBuilder;
import au.com.glob.clodmc.modules.bluemap.BlueMap;
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

@SuppressWarnings({"UnstableApiUsage"})
@NullMarked
public class ModuleRegistry implements Iterable<Module>, PluginBootstrap {
  private final Map<Class<? extends Module>, Module> modules = new HashMap<>();

  @Override
  public void bootstrap(BootstrapContext context) {
    VeinMiner.bootstrap(new BootstrapContextHelper(context));
  }

  public void registerAll() {
    // core - used by other modules
    this.register(OpAlerts.class);

    // crafting
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

  private void register(Class<? extends Module> moduleClass, String... requiredPlugins) {
    for (String plugin : requiredPlugins) {
      if (!Bukkit.getPluginManager().isPluginEnabled(plugin)) {
        Logger.warning(
            "Cannot load module "
                + moduleClass.getSimpleName()
                + ": depends on plugin "
                + plugin
                + " which is not enabled");
        return;
      }
    }

    Module module;
    try {
      module = moduleClass.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    this.modules.put(moduleClass, module);

    if (module instanceof Listener listener) {
      Bukkit.getServer().getPluginManager().registerEvents(listener, ClodMC.instance);
    }
  }

  @Override
  public Iterator<Module> iterator() {
    return this.modules.values().iterator();
  }

  @SuppressWarnings("unchecked")
  public @Nullable <T extends Module> T get(Class<T> moduleClass) {
    Module module = this.modules.get(moduleClass);
    return (T) module;
  }
}
