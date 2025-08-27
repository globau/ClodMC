package au.com.glob.clodmc.modules.inventory;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.annotations.Audience;
import au.com.glob.clodmc.annotations.Doc;
import au.com.glob.clodmc.command.CommandBuilder;
import au.com.glob.clodmc.command.CommandError;
import au.com.glob.clodmc.command.CommandUsageError;
import au.com.glob.clodmc.command.EitherCommandSender;
import au.com.glob.clodmc.datafile.DataFile;
import au.com.glob.clodmc.datafile.PlayerDataFile;
import au.com.glob.clodmc.datafile.PlayerDataFiles;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Chat;
import au.com.glob.clodmc.util.Logger;
import au.com.glob.clodmc.util.Players;
import au.com.glob.clodmc.util.Schedule;
import au.com.glob.clodmc.util.TimeUtil;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@Doc(
    audience = Audience.ADMIN,
    title = "Inventory Restore",
    description = "Automatic inventory backup, allowing admins to restore")
@NullMarked
public class InventoryRestore implements Module, Listener {
  private static final DateTimeFormatter SHORT_DATETIME_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
  private static final DateTimeFormatter LONG_DATETIME_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final String SEPARATOR = "-inv-";
  private static final String SUFFIX = ".bak";
  private static final int UUID_LEN = 36;
  private static final int PREFIX_LEN = UUID_LEN + SEPARATOR.length();
  private static final int SUFFIX_LEN = SUFFIX.length();
  private static final int MAX_BACKUP_COUNT = 5;

  private final File backupPath;

  // register inventory restore commands and set up backup system
  public InventoryRestore() {
    this.backupPath = ClodMC.instance.getDataFolder().toPath().resolve("players").toFile();

    CommandBuilder.build("restore_inv")
        .usage("/restore_inv <player> [backup]")
        .description("Restore player's inventory from automatic backups")
        .requiresOp()
        .executor(
            (EitherCommandSender sender, @Nullable String playerName, @Nullable String backup) -> {
              if (playerName == null) {
                if (!sender.isPlayer()) {
                  throw new CommandUsageError();
                }
                this.restorePlayerInventory(sender, sender.asPlayer(), backup);
                return;
              }

              UUID uuid = Players.getWhitelistedUUID(playerName);
              if (uuid == null) {
                throw new CommandError("Unknown player: %s".formatted(playerName));
              }
              Player player = Bukkit.getPlayer(uuid);
              if (player != null) {
                // player is online
                this.restorePlayerInventory(sender, player, backup);
                return;
              }

              // player is offline - set up to restore the next time they log in
              File backupFile = this.getBackupFile(uuid, backup);
              if (backupFile == null) {
                Chat.error(sender, "Failed to find backup file");
                return;
              }

              PlayerDataFile dataFile = PlayerDataFiles.of(uuid);
              dataFile.set("restore_inv", getBackupName(backupFile));
              dataFile.save();

              try {
                LocalDateTime date =
                    LocalDateTime.parse(getBackupName(backupFile), SHORT_DATETIME_FORMAT);
                Chat.info(
                    sender,
                    "Inventory for %s will be restored from %s next time they log in"
                        .formatted(dataFile.getPlayerName(), date.format(LONG_DATETIME_FORMAT)));
              } catch (DateTimeParseException e) {
                Chat.info(
                    sender,
                    "Inventory for %s will be restored next time they log in"
                        .formatted(dataFile.getPlayerName()));
              }
            })
        .completor(
            (CommandSender sender, List<String> args) -> {
              if (args.size() == 1) {
                // player name
                return Players.getWhitelisted().keySet().stream()
                    .filter(
                        (String name) ->
                            name.toLowerCase(Locale.ENGLISH)
                                .startsWith(args.getFirst().toLowerCase(Locale.ENGLISH)))
                    .toList();
              }
              if (args.size() == 2) {
                // backup name
                UUID uuid = Players.getWhitelistedUUID(args.getFirst());
                return uuid == null ? List.of() : this.getBackupNames(uuid);
              }
              return List.of();
            });
  }

  // backup inventory when player dies
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(PlayerDeathEvent event) {
    this.backupPlayerInventory(event.getPlayer(), false);
  }

  // if requested restore inventory backup when player joins
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerJoin(PlayerJoinEvent event) {
    PlayerDataFile dataFile = PlayerDataFiles.of(event.getPlayer());
    String backupName = dataFile.getString("restore_inv");
    if (backupName != null) {
      this.restorePlayerInventory(null, event.getPlayer(), backupName);
      dataFile.remove("restore_inv");
      dataFile.save();
    }
  }

  // backup inventory when /clear is used
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
    if (event.getPlayer().isOp() && event.getMessage().equals("/clear")) {
      this.backupPlayerInventory(event.getPlayer(), true);
    }
  }

  // save player's current inventory to backup file
  private void backupPlayerInventory(Player player, boolean notify) {
    PlayerInventory inv = player.getInventory();
    if (inv.isEmpty()) {
      return;
    }

    // save inv as array of slots
    File backupFile = this.buildBackupFile(player.getUniqueId());
    DataFile dataFile = new DataFile(backupFile);
    for (int i = 0; i < inv.getSize(); i++) {
      dataFile.set("slot.%d".formatted(i), inv.getItem(i));
    }
    dataFile.save();

    String message =
        "saved %s inventory as %s".formatted(player.getName(), getBackupName(backupFile));
    Logger.info(message);
    if (notify) {
      Chat.info(player, message);
    }

    // delete excess backups
    Schedule.asynchronously(
        () -> {
          List<File> backupFiles = this.getBackupFiles(player.getUniqueId());
          while (backupFiles.size() > MAX_BACKUP_COUNT) {
            File file = backupFiles.removeLast();
            if (!file.delete()) {
              Logger.error("failed to delete: %s".formatted(file));
            }
          }
        });
  }

  // restore player inventory from backup file
  private void restorePlayerInventory(
      @Nullable EitherCommandSender sender, Player player, @Nullable String backupName) {
    Schedule.asynchronously(
        () -> {
          File backupFile = this.getBackupFile(player.getUniqueId(), backupName);
          if (backupFile == null) {
            String message =
                backupName == null
                    ? "Failed to find any inventory backups for %s".formatted(player.getName())
                    : "Failed to find inventory backup for %s named %s"
                        .formatted(player.getName(), backupName);
            if (sender == null) {
              Logger.error(message);
            } else {
              Chat.error(sender, message);
            }
            return;
          }

          Schedule.onMainThread(
              () -> {
                PlayerInventory inv = player.getInventory();

                DataFile dataFile = new DataFile(backupFile);
                for (int i = 0; i < inv.getSize(); i++) {
                  inv.setItem(i, (ItemStack) dataFile.get("slot.%d".formatted(i)));
                }

                LocalDateTime date =
                    LocalDateTime.parse(getBackupName(backupFile), SHORT_DATETIME_FORMAT);
                if (sender == null) {
                  Chat.info(
                      player,
                      "Your inventory from %s has been restored"
                          .formatted(date.format(LONG_DATETIME_FORMAT)));
                } else {
                  Chat.info(
                      sender,
                      "Inventory for %s restored from %s"
                          .formatted(player.getName(), date.format(LONG_DATETIME_FORMAT)));
                }
              });
        });
  }

  private List<File> getBackupFiles(UUID uuid) {
    String prefix = "%s%s".formatted(uuid, SEPARATOR);
    File[] files =
        this.backupPath.listFiles(
            (File file) -> file.getName().startsWith(prefix) && file.getName().endsWith(SUFFIX));

    // must return mutable lists
    if (files == null) {
      return new ArrayList<>();
    }
    // newest first
    Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
    return new ArrayList<>(Arrays.asList(files));
  }

  private List<String> getBackupNames(UUID uuid) {
    return this.getBackupFiles(uuid).stream().map(InventoryRestore::getBackupName).toList();
  }

  private static String getBackupName(File file) {
    return file.getName().substring(PREFIX_LEN, file.getName().length() - SUFFIX_LEN);
  }

  private @Nullable File getBackupFile(UUID uuid, @Nullable String name) {
    List<File> backups = this.getBackupFiles(uuid);
    if (backups.isEmpty()) {
      return null;
    }
    return name == null
        ? backups.getFirst()
        : backups.stream()
            .filter((File file) -> getBackupName(file).equals(name))
            .max(Comparator.comparingLong(File::lastModified))
            .orElse(null);
  }

  private File buildBackupFile(UUID uuid) {
    return new File(
        this.backupPath,
        "%s%s%s%s"
            .formatted(uuid, SEPARATOR, TimeUtil.localNow().format(SHORT_DATETIME_FORMAT), SUFFIX));
  }
}
