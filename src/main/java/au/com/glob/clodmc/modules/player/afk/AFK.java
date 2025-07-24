package au.com.glob.clodmc.modules.player.afk;

import au.com.glob.clodmc.command.CommandBuilder;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Schedule;
import au.com.glob.clodmc.util.StringUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jspecify.annotations.NullMarked;

/** Automatic and manual afk; players are visibly afk in the tab-list */
@NullMarked
public class AFK implements Listener, Module {
  @SuppressWarnings({"NotNullFieldNotInitialized", "NullAway.Init"})
  public static AFK instance;

  private static final int IDLE_TIME = 300; // seconds
  private static final int CHECK_INTERVAL = 5; // seconds

  private final HashMap<UUID, PlayerState> playerStates = new HashMap<>();

  public AFK() {
    instance = this;

    CommandBuilder.build("afk")
        .description("Toggle AFK status")
        .executor(
            (Player player) -> {
              PlayerState playerState = this.playerStates.get(player.getUniqueId());
              if (playerState != null) {
                playerState.toggleAway();
              }
            });

    // ensure afk team exists and is empty on startup
    Team afkTeam = this.getAfkTeam();
    afkTeam.removeEntries(afkTeam.getEntries());
  }

  Team getAfkTeam() {
    Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    Team team = scoreboard.getTeam("AFK");
    if (team == null) {
      team = scoreboard.registerNewTeam("AFK");
      team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
      team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.ALWAYS);
      team.suffix(StringUtil.asComponent(" <grey>(afk)</grey>"));
    }
    return team;
  }

  @Override
  public void loadConfig() {
    // check for away players every second
    Schedule.periodically(
        CHECK_INTERVAL * 20,
        CHECK_INTERVAL * 20,
        () -> {
          long now = System.currentTimeMillis() / 1000;
          for (PlayerState playerState : this.playerStates.values()) {
            if (playerState.player.isOnline() && !playerState.isAway) {
              if (now - playerState.lastInteractionTime >= IDLE_TIME) {
                playerState.setAway(true);
              }
            }
          }
        });
  }

  // events

  private void onAction(Player player) {
    PlayerState playerState = this.playerStates.get(player.getUniqueId());
    if (playerState != null) {
      playerState.onAction();
    }
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    PlayerState playerState = new PlayerState(event.getPlayer());
    playerState.setBack(false);
    this.playerStates.put(event.getPlayer().getUniqueId(), playerState);
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    this.onAction(event.getPlayer());
  }

  @EventHandler
  public void onAsyncChat(AsyncChatEvent event) {
    Schedule.nextTick(() -> AFK.this.onAction(event.getPlayer()));
  }

  @EventHandler
  public void onPlayerMove(PlayerMoveEvent event) {
    if (event.hasChangedBlock()) {
      this.onAction(event.getPlayer());
    }
  }

  @EventHandler
  public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
    if (event.getDamager() instanceof Player player) {
      this.onAction(player);
    }
  }

  @EventHandler
  public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
    if (event.getMessage().equals("/afk") || event.getMessage().startsWith("/afk ")) {
      return;
    }
    this.onAction(event.getPlayer());
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    this.onAction(event.getPlayer());
  }

  @EventHandler
  public void onBlockPlace(BlockPlaceEvent event) {
    this.onAction(event.getPlayer());
  }

  @EventHandler
  public void onBlockBreak(BlockBreakEvent event) {
    this.onAction(event.getPlayer());
  }
}
