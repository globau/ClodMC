package au.com.glob.clodmc.modules;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.event.RegistryFreezeEvent;
import io.papermc.paper.registry.tag.TagKey;
import io.papermc.paper.tag.PostFlattenTagRegistrar;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public class BootstrapContextHelper {
  private final @NotNull BootstrapContext context;

  public BootstrapContextHelper(@NotNull BootstrapContext context) {
    this.context = context;
  }

  public void enchantment(
      @NotNull TypedKey<Enchantment> key, @NotNull Consumer<EnchantmentBuilder> handler) {
    EnchantmentBuilder builder = new EnchantmentBuilder(this.context, key);
    handler.accept(builder);
    builder.build();
  }

  @SuppressWarnings("UnusedReturnValue")
  public static class EnchantmentBuilder {
    private final @NotNull BootstrapContext context;
    private final @NotNull TypedKey<Enchantment> key;
    private @Nullable String description;
    private @Nullable TagKey<ItemType> supportedItems;
    private int weight = -1;
    private int maxLevel = -1;
    private int minCost = -1;
    private int maxCost = -1;
    private int anvilCost = -1;
    private @Nullable EquipmentSlotGroup activeSlot;
    private @Nullable List<TagKey<Enchantment>> tags;

    public EnchantmentBuilder(
        @NotNull BootstrapContext context, @NotNull TypedKey<Enchantment> key) {
      this.context = context;
      this.key = key;
    }

    public @NotNull EnchantmentBuilder description(@NotNull String value) {
      this.description = value;
      return this;
    }

    public @NotNull EnchantmentBuilder supportedItems(@NotNull TagKey<ItemType> value) {
      this.supportedItems = value;
      return this;
    }

    public @NotNull EnchantmentBuilder weight(int value) {
      this.weight = value;
      return this;
    }

    public @NotNull EnchantmentBuilder maxLevel(int value) {
      this.maxLevel = value;
      return this;
    }

    public @NotNull EnchantmentBuilder cost(int minValue, int maxValue) {
      this.minCost = minValue;
      this.maxCost = maxValue;
      return this;
    }

    public @NotNull EnchantmentBuilder anvilCost(int value) {
      this.anvilCost = value;
      return this;
    }

    public @NotNull EnchantmentBuilder activeSlot(@NotNull EquipmentSlotGroup value) {
      this.activeSlot = value;
      return this;
    }

    public @NotNull EnchantmentBuilder tags(@NotNull List<TagKey<Enchantment>> values) {
      this.tags = values;
      return this;
    }

    protected void build() {
      LifecycleEventManager<@NotNull BootstrapContext> manager = this.context.getLifecycleManager();
      manager.registerEventHandler(
          RegistryEvents.ENCHANTMENT
              .freeze()
              .newHandler(
                  (RegistryFreezeEvent<Enchantment, EnchantmentRegistryEntry.@NotNull Builder>
                          event) ->
                      event
                          .registry()
                          .register(
                              this.key,
                              (EnchantmentRegistryEntry.@NotNull Builder builder) -> {
                                if (this.description != null) {
                                  builder.description(
                                      Component.translatable(
                                          "enchantment." + this.key.value(), this.description));
                                }
                                if (this.supportedItems != null) {
                                  builder.supportedItems(event.getOrCreateTag(this.supportedItems));
                                }
                                if (this.weight != -1) {
                                  builder.weight(this.weight);
                                }
                                if (this.maxLevel != -1) {
                                  builder.maxLevel(this.maxLevel);
                                }
                                if (this.minCost != -1) {
                                  builder.minimumCost(
                                      EnchantmentRegistryEntry.EnchantmentCost.of(this.minCost, 0));
                                }
                                if (this.maxCost != -1) {
                                  builder.maximumCost(
                                      EnchantmentRegistryEntry.EnchantmentCost.of(this.maxCost, 0));
                                }
                                if (this.anvilCost != -1) {
                                  builder.anvilCost(this.anvilCost);
                                }
                                if (this.activeSlot != null) {
                                  builder.activeSlots(this.activeSlot);
                                }
                              })));

      if (this.tags != null) {
        manager.registerEventHandler(
            LifecycleEvents.TAGS.postFlatten(RegistryKey.ENCHANTMENT),
            (ReloadableRegistrarEvent<@NotNull PostFlattenTagRegistrar<Enchantment>> event) -> {
              PostFlattenTagRegistrar<Enchantment> registrar = event.registrar();
              Set<@NotNull TypedKey<Enchantment>> keySet = Set.of(this.key);
              for (TagKey<Enchantment> tag : this.tags) {
                registrar.addToTag(tag, keySet);
              }
            });
      }
    }
  }
}
