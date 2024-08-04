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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("deprecation")
public class EitherCommandSender implements CommandSender {
  private final @NotNull CommandSender sender;

  public EitherCommandSender(@NotNull CommandSender sender) {
    this.sender = sender;
  }

  @Override
  public @NotNull Component name() {
    return this.sender.name();
  }

  @Override
  public @NotNull String getName() {
    return this.sender.getName();
  }

  public boolean isPlayer() {
    return this.sender instanceof Player;
  }

  public @NotNull Player asPlayer() {
    assert this.sender instanceof Player;
    return (Player) this.sender;
  }

  public boolean is(@NotNull CommandSender sender) {
    return this.sender.equals(sender);
  }

  @Override
  public void sendMessage(@NotNull String message) {
    this.sender.sendMessage(message);
  }

  @Override
  public void sendMessage(@NotNull String... messages) {
    this.sender.sendMessage(messages);
  }

  @Override
  public void sendMessage(@Nullable UUID sender, @NotNull String message) {
    this.sender.sendMessage(sender, message);
  }

  @Override
  public void sendMessage(@Nullable UUID sender, @NotNull String... messages) {
    this.sender.sendMessage(sender, messages);
  }

  @Override
  public @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin) {
    return this.sender.addAttachment(plugin);
  }

  @Override
  public @NotNull PermissionAttachment addAttachment(
      @NotNull Plugin plugin, @NotNull String name, boolean value) {
    return this.sender.addAttachment(plugin, name, value);
  }

  @Override
  public @Nullable PermissionAttachment addAttachment(@NotNull Plugin plugin, int ticks) {
    return this.sender.addAttachment(plugin, ticks);
  }

  @Override
  public @Nullable PermissionAttachment addAttachment(
      @NotNull Plugin plugin, @NotNull String name, boolean value, int ticks) {
    return this.sender.addAttachment(plugin, name, value, ticks);
  }

  @Override
  public void removeAttachment(@NotNull PermissionAttachment attachment) {
    this.sender.removeAttachment(attachment);
  }

  @Override
  public @NotNull Set<PermissionAttachmentInfo> getEffectivePermissions() {
    return this.sender.getEffectivePermissions();
  }

  @Override
  public boolean hasPermission(@NotNull Permission perm) {
    return this.sender.hasPermission(perm);
  }

  @Override
  public boolean hasPermission(@NotNull String name) {
    return this.sender.hasPermission(name);
  }

  @Override
  public boolean isPermissionSet(@NotNull Permission perm) {
    return this.sender.isPermissionSet(perm);
  }

  @Override
  public boolean isPermissionSet(@NotNull String name) {
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
  public void setOp(boolean value) {
    this.sender.setOp(value);
  }

  @Override
  public @NotNull Server getServer() {
    return this.sender.getServer();
  }

  @Override
  public @NotNull Spigot spigot() {
    return this.sender.spigot();
  }
}
