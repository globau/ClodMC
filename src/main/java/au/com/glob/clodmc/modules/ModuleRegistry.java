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
import au.com.glob.clodmc.modules.player.afk.AFK;
import au.com.glob.clodmc.modules.player.offlinemessages.OfflineMessages;
import au.com.glob.clodmc.modules.player.welcomebook.WelcomeBookAdmin;
import au.com.glob.clodmc.modules.player.welcomebook.WelcomeBookPlayer;
import au.com.glob.clodmc.modules.server.ClodServerLinks;
import au.com.glob.clodmc.modules.server.MOTD;
import au.com.glob.clodmc.modules.server.Permissions;
import au.com.glob.clodmc.modules.server.RequiredPlugins;
import au.com.glob.clodmc.modules.server.ServerStatus;
import au.com.glob.clodmc.modules.server.heapmap.HeatMap;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import org.jspecify.annotations.NullMarked;

/** registry for all plugin modules with automatic registration and lifecycle management */
@SuppressWarnings({"UnstableApiUsage"})
@NullMarked
public class ModuleRegistry implements PluginBootstrap {
  // paper bootstrap lifecycle hook for early module registration
  @Override
  public void bootstrap(final BootstrapContext context) {
    VeinMiner.bootstrap(new BootstrapContextHelper(context));
  }

  // register all modules organised by category
  public static void registerAll() {
    // custom event listeners
    ClodMC.registerListener(new PlayerTargetBlockListener());

    // core - used by other modules
    Module.create(OpAlerts::new);

    // crafting
    Module.create(ChorusFlower::new);
    Module.create(SporeBlossom::new);

    // interactions
    Module.create(FastLeafDecay::new);
    Module.create(Gateways::new);
    Module.create(NamedStorage::new);
    Module.create(SignedContainers::new);
    Module.create(VeinMiner::new);
    Module.create(WaxedItemFrames::new);
    Module.create(WaxedPressurePlates::new);

    // inventory
    Module.create(AdminInv::new);
    Module.create(DeepPockets::new);
    Module.create(InventoryRestore::new);
    Module.create(InventorySort::new);

    // mobs
    Module.create(BetterDrops::new);
    Module.create(ExplodingCreepers::new);
    Module.create(PreventMobGriefing::new);
    Module.create(PreventMobSpawn::new);
    Module.create(SilenceMobs::new);

    // player
    Module.create(AFK::new);
    Module.create(Back::new);
    Module.create(DeathLog::new);
    Module.create(GameMode::new);
    Module.create(Homes::new);
    Module.create(Invite::new);
    Module.create(OfflineMessages::new);
    Module.create(PlayerTracker::new);
    Module.create(Seen::new);
    Module.create(Sleep::new);
    Module.create(Spawn::new);
    Module.create(WelcomeBookAdmin::new);
    Module.create(WelcomeBookPlayer::new);

    // server
    Module.create(ClodServerLinks::new);
    Module.create(HeatMap::new);
    Module.create(MOTD::new);
    Module.create(Permissions::new);
    Module.create(RequiredPlugins::new);
    Module.create(ServerStatus::new);

    // bluemap
    Module.create(BlueMap::new);

    // register commands built by modules
    CommandBuilder.registerBuilders();
  }
}
