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

public class AFK extends SimpleCommand implements Listener, Module {
  private final HashMap<UUID, PlayerState> playerStates = new HashMap<>();
  private final int idleTime;

  public AFK() {
    super("afk", "toggle afk status");
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
  public void onPlayerChat(AsyncChatEvent event) {
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
    private final Player player;
    private Team scoreboardTeam;
    private long lastInteractionTime;
    private BlockPos awayBlockPos;
    private boolean isAway;

    private PlayerState(@NotNull Player player) {
      this.player = player;
      this.lastInteractionTime = System.currentTimeMillis() / 1000;
      this.isAway = false;

      // use scoreboard to append "(afk)" to the player's name
      Scoreboard scoreboard = this.player.getScoreboard();
      this.scoreboardTeam = scoreboard.getTeam("AFK");
      if (this.scoreboardTeam == null) {
        this.scoreboardTeam = scoreboard.registerNewTeam("AFK");
        this.scoreboardTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        this.scoreboardTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.ALWAYS);
        this.scoreboardTeam.suffix(MiniMessage.miniMessage().deserialize(" <grey>(afk)</grey>"));
      }
    }

    public void onMove() {
      if (this.isAway) {
        // clear afk only when the player moves to a different block
        // ie. looking around shouldn't clear afk status
        BlockPos playerPos = BlockPos.of(this.player.getLocation());
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
          player.sendRichMessage(
              this.isAway ? "<grey>You are now AFK" : "<grey>You are no longer AFK");
        } else {
          player.sendRichMessage(
              this.isAway
                  ? "<grey>" + this.player.getName() + " is now AFK"
                  : "<grey>" + this.player.getName() + " is no longer AFK");
        }
      }
    }
  }
}
