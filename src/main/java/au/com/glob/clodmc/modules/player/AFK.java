package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.command.CommandBuilder;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Chat;
import au.com.glob.clodmc.util.Schedule;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.HashMap;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.MiniMessage;
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
import org.jetbrains.annotations.NotNull;

/** Automatic and manual afk; players are visibly afk in the tab-list */
public class AFK implements Listener, Module {
  @SuppressWarnings("NotNullFieldNotInitialized")
  public static @NotNull AFK instance;

  private static final int IDLE_TIME = 300; // seconds
  private static final int CHECK_INTERVAL = 5; // seconds

  private final @NotNull HashMap<UUID, PlayerState> playerStates = new HashMap<>();

  public AFK() {
    instance = this;

    CommandBuilder.build(
        "afk",
        (CommandBuilder builder) -> {
          builder.description("Toggle AFK status");
          builder.executor(
              (@NotNull Player player) -> {
                PlayerState playerState = this.playerStates.get(player.getUniqueId());
                playerState.toggleAway();
              });
        });

    // ensure afk team exists and is empty on startup
    Team afkTeam = this.getAfkTeam();
    afkTeam.removeEntries(afkTeam.getEntries());
  }

  private @NotNull Team getAfkTeam() {
    Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    Team team = scoreboard.getTeam("AFK");
    if (team == null) {
      team = scoreboard.registerNewTeam("AFK");
      team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
      team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.ALWAYS);
      team.suffix(MiniMessage.miniMessage().deserialize(" <grey>(afk)</grey>"));
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

  @EventHandler
  public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
    PlayerState playerState = new PlayerState(event.getPlayer());
    playerState.setBack(false);
    this.playerStates.put(event.getPlayer().getUniqueId(), playerState);
  }

  @EventHandler
  public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
    PlayerState playerState = this.playerStates.get(event.getPlayer().getUniqueId());
    playerState.setBack(false);
    this.playerStates.remove(event.getPlayer().getUniqueId());
  }

  @EventHandler
  public void onAsyncChat(@NotNull AsyncChatEvent event) {
    Schedule.nextTick(() -> AFK.this.playerStates.get(event.getPlayer().getUniqueId()).onAction());
  }

  @EventHandler
  public void onPlayerMove(@NotNull PlayerMoveEvent event) {
    if (event.hasChangedBlock()) {
      this.playerStates.get(event.getPlayer().getUniqueId()).onAction();
    }
  }

  @EventHandler
  public void onEntityDamageByEntity(@NotNull EntityDamageByEntityEvent event) {
    if (event.getDamager() instanceof Player player) {
      this.playerStates.get(player.getUniqueId()).onAction();
    }
  }

  @EventHandler
  public void onPlayerCommandPreprocess(@NotNull PlayerCommandPreprocessEvent event) {
    if (event.getMessage().equals("/afk") || event.getMessage().startsWith("/afk ")) {
      return;
    }
    this.playerStates.get(event.getPlayer().getUniqueId()).onAction();
  }

  @EventHandler
  public void onPlayerInteract(@NotNull PlayerInteractEvent event) {
    this.playerStates.get(event.getPlayer().getUniqueId()).onAction();
  }

  @EventHandler
  public void onBlockPlace(@NotNull BlockPlaceEvent event) {
    this.playerStates.get(event.getPlayer().getUniqueId()).onAction();
  }

  @EventHandler
  public void onBlockBreak(@NotNull BlockBreakEvent event) {
    this.playerStates.get(event.getPlayer().getUniqueId()).onAction();
  }

  private static final class PlayerState {
    private final @NotNull Player player;
    private long lastInteractionTime;
    private boolean isAway;

    private PlayerState(@NotNull Player player) {
      this.player = player;
      this.lastInteractionTime = System.currentTimeMillis() / 1000;
      this.isAway = false;
    }

    public void onAction() {
      this.lastInteractionTime = System.currentTimeMillis() / 1000;
      if (this.isAway) {
        this.setBack(true);
      }
    }

    public void toggleAway() {
      if (this.isAway) {
        this.onAction();
      } else {
        this.setAway(true);
      }
    }

    public void setAway(boolean announce) {
      this.isAway = true;
      AFK.instance.getAfkTeam().addEntry(this.player.getName());
      if (announce) {
        this.announce();
      }
    }

    public void setBack(boolean announce) {
      this.isAway = false;
      AFK.instance.getAfkTeam().removeEntry(this.player.getName());
      if (announce) {
        this.announce();
      }
    }

    private void announce() {
      for (Player player : Bukkit.getOnlinePlayers()) {
        if (player.equals(this.player)) {
          Chat.fyi(player, this.isAway ? "You are now AFK" : "You are no longer AFK");
        } else {
          Chat.fyi(
              player,
              this.isAway
                  ? this.player.getName() + " is now AFK"
                  : this.player.getName() + " is no longer AFK");
        }
      }
    }
  }
}
