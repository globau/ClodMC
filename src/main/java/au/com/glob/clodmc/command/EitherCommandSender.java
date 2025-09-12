package au.com.glob.clodmc.command;

import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** wrapper for command sender that can be either a player or console */
@NullMarked
public class EitherCommandSender implements CommandSender {
  private final CommandSender sender;

  public EitherCommandSender(final CommandSender sender) {
    this.sender = sender;
  }

  @Override
  public Component name() {
    return this.sender.name();
  }

  @Override
  public String getName() {
    return this.sender.getName();
  }

  // check if the sender is a player
  public boolean isPlayer() {
    return this.sender instanceof Player;
  }

  // cast sender to player (requires isPlayer() check first)
  public Player asPlayer() {
    assert this.sender instanceof Player;
    return (Player) this.sender;
  }

  // check if this wrapper contains the given sender
  public boolean is(final CommandSender sender) {
    return this.sender.equals(sender);
  }

  @Override
  public void sendMessage(final String message) {
    this.sender.sendMessage(message);
  }

  @Override
  public void sendMessage(final String... messages) {
    this.sender.sendMessage(messages);
  }

  @Override
  @Deprecated
  public void sendMessage(@Nullable final UUID sender, final String message) {
    this.sender.sendPlainMessage(message);
  }

  @Override
  @Deprecated
  public void sendMessage(@Nullable final UUID sender, final String... messages) {
    for (final String message : messages) {
      this.sendMessage(message);
    }
  }

  @Override
  public PermissionAttachment addAttachment(final Plugin plugin) {
    return this.sender.addAttachment(plugin);
  }

  @Override
  public PermissionAttachment addAttachment(
      final Plugin plugin, final String name, final boolean value) {
    return this.sender.addAttachment(plugin, name, value);
  }

  @Override
  public @Nullable PermissionAttachment addAttachment(final Plugin plugin, final int ticks) {
    return this.sender.addAttachment(plugin, ticks);
  }

  @Override
  public @Nullable PermissionAttachment addAttachment(
      final Plugin plugin, final String name, final boolean value, final int ticks) {
    return this.sender.addAttachment(plugin, name, value, ticks);
  }

  @Override
  public void removeAttachment(final PermissionAttachment attachment) {
    this.sender.removeAttachment(attachment);
  }

  @Override
  public Set<PermissionAttachmentInfo> getEffectivePermissions() {
    return this.sender.getEffectivePermissions();
  }

  @Override
  public boolean hasPermission(final Permission perm) {
    return this.sender.hasPermission(perm);
  }

  @Override
  public boolean hasPermission(final String name) {
    return this.sender.hasPermission(name);
  }

  @Override
  public boolean isPermissionSet(final Permission perm) {
    return this.sender.isPermissionSet(perm);
  }

  @Override
  public boolean isPermissionSet(final String name) {
    return this.sender.isPermissionSet(name);
  }

  @Override
  public void recalculatePermissions() {
    this.sender.recalculatePermissions();
  }

  @Override
  public boolean isOp() {
    return this.sender.isOp();
  }

  @Override
  public void setOp(final boolean value) {
    this.sender.setOp(value);
  }

  @Override
  public Server getServer() {
    return this.sender.getServer();
  }

  @Override
  public Spigot spigot() {
    return this.sender.spigot();
  }
}
