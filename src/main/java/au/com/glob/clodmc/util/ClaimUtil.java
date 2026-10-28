package au.com.glob.clodmc.util;

import java.util.function.Supplier;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** griefprevention claim permission helpers */
@NullMarked
public final class ClaimUtil {
  private ClaimUtil() {}

  // reason the player can't open the container, or null if allowed; mirrors griefprevention's
  // container rules, which only apply when the container itself is clicked
  public static @Nullable String containerDenialReason(
      final Player player, final Block block, @Nullable final Event event) {
    final GriefPrevention griefPrevention = GriefPrevention.instance;
    if (!griefPrevention.config_claims_preventTheft) {
      return null;
    }

    final ClaimPermission permission =
        griefPrevention.config_claims_hoppersRequireBuildTrust && block.getType() == Material.HOPPER
            ? ClaimPermission.Build
            : ClaimPermission.Container;
    return denialReason(player, block, permission, event);
  }

  // reason the player lacks the permission at the block's claim, or null if allowed
  public static @Nullable String denialReason(
      final Player player,
      final Block block,
      final ClaimPermission permission,
      @Nullable final Event event) {
    final Claim claim =
        GriefPrevention.instance.dataStore.getClaimAt(block.getLocation(), false, null);
    if (claim == null) {
      return null;
    }
    final Supplier<@Nullable String> denial = claim.checkPermission(player, permission, event);
    return denial == null ? null : denial.get();
  }
}
