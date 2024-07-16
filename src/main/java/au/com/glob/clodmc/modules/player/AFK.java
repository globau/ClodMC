package au.com.glob.clodmc.modules.player;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.modules.SimpleCommand;
import au.com.glob.clodmc.util.BlockPos;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
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
import org.jetbrains.annotations.Nullable;

public class AFK extends SimpleCommand implements Listener, Module {
  private final @NotNull HashMap<UUID, PlayerState> playerStates = new HashMap<>();
  private int idleTime = 300;

  public AFK() {
    super("afk", "toggle afk status");
  }

  @Override
  public void loadConfig() {
    this.idleTime = ClodMC.instance.getConfig().getInt("afk.idle-time", 300);

    // check for away players every second
    Bukkit.getScheduler()
        .runTaskTimer(
            ClodMC.instance,
            () -> {
              long now = System.currentTimeMillis() / 1000;
              for (PlayerState playerState : this.playerStates.values()) {
                if (playerState.player.isOnline() && !playerState.isAway) {
                  if (now - playerState.lastInteractionTime >= this.idleTime) {
                    playerState.setAway(true);
                  }
                }
              }
            },
            20,
            20);
  }

  @Override
  protected void execute(@NotNull CommandSender sender, @NotNull List<String> args) {
    // `/afk` command --> toggle
    Player player = this.toPlayer(sender);
    PlayerState playerState = this.playerStates.get(player.getUniqueId());
    playerState.toggleAway();
  }

  // events

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    this.playerStates.put(event.getPlayer().getUniqueId(), new PlayerState(event.getPlayer()));
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    PlayerState playerState = this.playerStates.get(event.getPlayer().getUniqueId());
    playerState.setBack(false);
    this.playerStates.remove(event.getPlayer().getUniqueId());
  }

  @EventHandler
  public void onPlayerChat(@NotNull AsyncChatEvent event) {
    Bukkit.getScheduler()
        .runTask(
            ClodMC.instance,
            () -> AFK.this.playerStates.get(event.getPlayer().getUniqueId()).onAction());
  }

  @EventHandler
  public void onPlayerMove(PlayerMoveEvent event) {
    this.playerStates.get(event.getPlayer().getUniqueId()).onMove();
  }

  @EventHandler
  public void onEntityDamage(EntityDamageByEntityEvent event) {
    if (event.getDamager() instanceof Player player) {
      this.playerStates.get(player.getUniqueId()).onAction();
    }
  }

  @EventHandler
  public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
    this.playerStates.get(event.getPlayer().getUniqueId()).onAction();
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    this.playerStates.get(event.getPlayer().getUniqueId()).onAction();
  }

  @EventHandler
  public void onPlayerBlockPlace(BlockPlaceEvent event) {
    this.playerStates.get(event.getPlayer().getUniqueId()).onAction();
  }

  @EventHandler
  public void onPlayerBlockBreak(BlockBreakEvent event) {
    this.playerStates.get(event.getPlayer().getUniqueId()).onAction();
  }

  private static final class PlayerState {
    private final @NotNull Player player;
    private final @NotNull Team scoreboardTeam;
    private long lastInteractionTime;
    private @Nullable BlockPos awayBlockPos;
    private boolean isAway;

    private PlayerState(@NotNull Player player) {
      this.player = player;
      this.lastInteractionTime = System.currentTimeMillis() / 1000;
      this.isAway = false;

      // use scoreboard to append "(afk)" to the player's name
      Scoreboard scoreboard = this.player.getScoreboard();
      Team scoreboardTeam = scoreboard.getTeam("AFK");
      if (scoreboardTeam == null) {
        scoreboardTeam = scoreboard.registerNewTeam("AFK");
        scoreboardTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        scoreboardTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.ALWAYS);
        scoreboardTeam.suffix(MiniMessage.miniMessage().deserialize(" <grey>(afk)</grey>"));
      }
      this.scoreboardTeam = scoreboardTeam;
    }

    public void onMove() {
      if (this.isAway) {
        // clear afk only when the player moves to a different block
        // ie. looking around shouldn't clear afk status
        BlockPos playerPos = BlockPos.of(this.player.getLocation());
        assert this.awayBlockPos != null;
        if (playerPos.equals(this.awayBlockPos)) {
          return;
        }
      }
      this.onAction();
    }

    public void onAction() {
      this.lastInteractionTime = System.currentTimeMillis() / 1000;
      if (this.isAway) {
        this.setBack(true);
      }
    }

    public void toggleAway() {
      if (this.isAway) {
        this.setBack(true);
      } else {
        this.setAway(true);
      }
    }

    public void setAway(boolean announce) {
      this.isAway = true;
      this.awayBlockPos = BlockPos.of(this.player.getLocation());
      this.scoreboardTeam.addEntry(this.player.getName());
      if (announce) {
        this.announce();
      }
    }

    public void setBack(boolean announce) {
      this.isAway = false;
      this.awayBlockPos = null;
      this.scoreboardTeam.removeEntry(this.player.getName());
      if (announce) {
        this.announce();
      }
    }

    private void announce() {
      for (Player player : Bukkit.getOnlinePlayers()) {
        if (player.equals(this.player)) {
          ClodMC.fyi(player, this.isAway ? "You are now AFK" : "You are no longer AFK");
        } else {
          ClodMC.fyi(
              player,
              this.isAway
                  ? this.player.getName() + " is now AFK"
                  : this.player.getName() + " is no longer AFK");
        }
      }
    }
  }
}
