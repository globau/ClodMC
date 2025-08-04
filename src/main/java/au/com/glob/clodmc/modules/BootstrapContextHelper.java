package au.com.glob.clodmc.modules;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryComposeEvent;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.tag.TagKey;
import io.papermc.paper.tag.PostFlattenTagRegistrar;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import org.bukkit.enchantments.Enchantment;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** wrapper around paper bootstrap context for easier enchantment and tag registration */
@SuppressWarnings("UnstableApiUsage")
@NullMarked
public class BootstrapContextHelper {
  private final LifecycleEventManager<BootstrapContext> manager;

  public BootstrapContextHelper(BootstrapContext context) {
    this.manager = context.getLifecycleManager();
  }

  // register custom enchantment with optional tags
  public void enchantment(
      TypedKey<Enchantment> key,
      @Nullable List<TagKey<Enchantment>> tags,
      BiConsumer<
              RegistryComposeEvent<Enchantment, EnchantmentRegistryEntry.Builder>,
              EnchantmentRegistryEntry.Builder>
          handler) {
    this.manager.registerEventHandler(
        RegistryEvents.ENCHANTMENT
            .compose()
            .newHandler(
                (RegistryComposeEvent<Enchantment, EnchantmentRegistryEntry.Builder> event) ->
                    event
                        .registry()
                        .register(
                            key,
                            (EnchantmentRegistryEntry.Builder builder) ->
                                handler.accept(event, builder))));
    if (tags != null) {
      this.manager.registerEventHandler(
          LifecycleEvents.TAGS.postFlatten(RegistryKey.ENCHANTMENT),
          (ReloadableRegistrarEvent<PostFlattenTagRegistrar<Enchantment>> event) -> {
            PostFlattenTagRegistrar<Enchantment> registrar = event.registrar();
            Set<TypedKey<Enchantment>> keySet = Set.of(key);
            for (TagKey<Enchantment> tag : Objects.requireNonNull(tags)) {
              registrar.addToTag(tag, keySet);
            }
          });
    }
  }
}
