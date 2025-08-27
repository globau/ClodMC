package au.com.glob.clodmc.modules.mobs;

import au.com.glob.clodmc.annotations.Audience;
import au.com.glob.clodmc.annotations.Doc;
import au.com.glob.clodmc.modules.Module;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.jspecify.annotations.NullMarked;

@Doc(
    audience = Audience.SERVER,
    title = "Exploding Creepers",
    description = "Adds a slim chance that a creeper explodes into fireworks",
    hidden = true)
@NullMarked
public class ExplodingCreepers implements Module, Listener {

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onEntityExplode(EntityExplodeEvent event) {
    Entity entity = event.getEntity();
    if (!(entity instanceof Creeper)) {
      return;
    }

    // 10% chance
    Random random = ThreadLocalRandom.current();
    if (random.nextDouble() > 0.1) {
      return;
    }

    Firework firework = entity.getWorld().spawn(event.getLocation().add(0, 0.2, 0), Firework.class);
    FireworkMeta meta = firework.getFireworkMeta();
    int count = random.nextInt(3) + 1;
    for (int i = 0; i < count; i++) {
      FireworkEffect.Builder builder =
          FireworkEffect.builder()
              .flicker(random.nextBoolean())
              .trail(random.nextBoolean())
              .with(FireworkEffect.Type.BURST)
              .withColor(getRandomColour(random));
      if (random.nextBoolean()) {
        builder.withFade(getRandomColour(random));
      }
      meta.addEffect(builder.build());
    }
    firework.setFireworkMeta(meta);
    firework.detonate();

    event.setCancelled(true);
  }

  private static Color getRandomColour(Random random) {
    return Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256));
  }
}
