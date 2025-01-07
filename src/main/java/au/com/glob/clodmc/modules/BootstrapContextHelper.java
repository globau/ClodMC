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
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public class BootstrapContextHelper {
  private final @NotNull BootstrapContext context;
  private final @NotNull LifecycleEventManager<@NotNull BootstrapContext> manager;

  public BootstrapContextHelper(@NotNull BootstrapContext context) {
    this.context = context;
    this.manager = this.context.getLifecycleManager();
  }

  public void enchantment(
      @NotNull TypedKey<Enchantment> key,
      @Nullable List<TagKey<Enchantment>> tags,
      @NotNull BiConsumer<
                  RegistryFreezeEvent<Enchantment, EnchantmentRegistryEntry.@NotNull Builder>,
                  EnchantmentRegistryEntry.Builder>
              handler) {
    this.manager.registerEventHandler(
        RegistryEvents.ENCHANTMENT
            .freeze()
            .newHandler(
                (RegistryFreezeEvent<Enchantment, EnchantmentRegistryEntry.@NotNull Builder>
                        event) ->
                    event
                        .registry()
                        .register(
                            key,
                            (EnchantmentRegistryEntry.@NotNull Builder builder) -> {
                              handler.accept(event, builder);
                            })));
    if (tags != null) {
      this.manager.registerEventHandler(
          LifecycleEvents.TAGS.postFlatten(RegistryKey.ENCHANTMENT),
          (ReloadableRegistrarEvent<@NotNull PostFlattenTagRegistrar<Enchantment>> event) -> {
            PostFlattenTagRegistrar<Enchantment> registrar = event.registrar();
            Set<@NotNull TypedKey<Enchantment>> keySet = Set.of(key);
            for (TagKey<Enchantment> tag : Objects.requireNonNull(tags)) {
              registrar.addToTag(tag, keySet);
            }
          });
    }
  }
}
