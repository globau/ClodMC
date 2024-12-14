package au.com.glob.clodmc.modules.interactions;

import au.com.glob.clodmc.modules.BootstrapContextHelper;
import au.com.glob.clodmc.modules.Module;
import au.com.glob.clodmc.util.Schedule;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys;
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.jetbrains.annotations.NotNull;

/** Mine connected blocks with one action */
@SuppressWarnings("UnstableApiUsage")
public class VeinMiner implements Module, Listener {
  @NotNull private static final TypedKey<Enchantment> VEINMINE_KEY =
      TypedKey.create(RegistryKey.ENCHANTMENT, Key.key("clod-mc:veinminer"));

  private static final int DELAY = 1;
  private static final int MAX_CHAIN = 100;
  private static final int COST = 1;
  private static final @NotNull List<BlockFace> FACES =
      List.of(
          BlockFace.NORTH,
          BlockFace.SOUTH,
          BlockFace.EAST,
          BlockFace.WEST,
          BlockFace.UP,
          BlockFace.DOWN);

  private final @NotNull Enchantment veinmineEnchantment;
  private final @NotNull Set<UUID> cooldownUUIDs = new HashSet<>();

  public static void bootstrap(@NotNull BootstrapContextHelper context) {
    context.enchantment(
        VEINMINE_KEY,
        (BootstrapContextHelper.EnchantmentBuilder builder) ->
            builder
                .description("Veinmine")
                .supportedItems(ItemTypeTagKeys.ENCHANTABLE_MINING)
                .weight(1)
                .maxLevel(1)
                .cost(15, 65)
                .anvilCost(1)
                .activeSlot(EquipmentSlotGroup.MAINHAND)
                .tags(
                    List.of(
                        EnchantmentTagKeys.TRADEABLE,
                        EnchantmentTagKeys.NON_TREASURE,
                        EnchantmentTagKeys.NON_TREASURE)));
  }

  public VeinMiner() {
    Registry<Enchantment> enchantmentRegistry =
        RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
    this.veinmineEnchantment = enchantmentRegistry.getOrThrow(VEINMINE_KEY);
  }

  @EventHandler
  public void onBlockBreak(@NotNull BlockBreakEvent event) {
    Player player = event.getPlayer();

    if (!player.isSneaking()) {
      return;
    }

    // check tool
    ItemStack tool = player.getInventory().getItemInMainHand();
    if (event.getBlock().getDrops(tool).isEmpty()
        || !tool.containsEnchantment(this.veinmineEnchantment)) {
      return;
    }

    // check cooldown
    if (this.cooldownUUIDs.contains(player.getUniqueId())) {
      return;
    }

    // break touching blocks
    this.breakBlocks(event.getBlock(), tool, new HashSet<>(MAX_CHAIN), player);

    // cooldown
    this.cooldownUUIDs.add(player.getUniqueId());
    Schedule.delayed(2 * 20, () -> this.cooldownUUIDs.remove(player.getUniqueId()));
  }

  private void breakBlocks(
      @NotNull Block block,
      @NotNull ItemStack tool,
      @NotNull Set<Block> processed,
      @NotNull Player player) {
    // don't break too many blocks
    if (processed.size() > MAX_CHAIN) {
      return;
    }

    // stop before breaking tools
    if (player.getGameMode().equals(GameMode.SURVIVAL)
        && tool.getItemMeta() instanceof Damageable damageable) {
      int damage = damageable.hasDamage() ? damageable.getDamage() : 0;
      int maxDamage =
          damageable.hasMaxDamage() ? damageable.getMaxDamage() : tool.getType().getMaxDurability();
      if (maxDamage - damage <= COST) {
        return;
      }
    }

    Material marterial = block.getType();

    // break this block, except for the initial block as that's handled by the BlockBreak event
    if (!processed.isEmpty()) {
      block.breakNaturally(tool, true, true);
      if (player.getGameMode().equals(GameMode.SURVIVAL)) {
        tool.damage(COST, player);
      }
    }

    processed.add(block);

    // break touching blocks in random order
    ArrayList<BlockFace> shuffledFaces = new ArrayList<>(FACES);
    Collections.shuffle(shuffledFaces);
    for (BlockFace face : shuffledFaces) {
      Block touchingBlock = block.getRelative(face);
      if (touchingBlock.getType().equals(marterial) && !processed.contains(touchingBlock)) {
        Schedule.delayed(DELAY, () -> this.breakBlocks(touchingBlock, tool, processed, player));
      }
    }
  }
}
