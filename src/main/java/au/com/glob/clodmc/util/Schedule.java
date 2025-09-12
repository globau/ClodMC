package au.com.glob.clodmc.util;

import au.com.glob.clodmc.ClodMC;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.NullMarked;

/** wrappers around Bukkit scheduling, with clearer and cleaner semantics */
@NullMarked
public final class Schedule {
  // schedule the task to run on the main thread
  public static void onMainThread(final Runnable task) {
    Bukkit.getScheduler().scheduleSyncDelayedTask(ClodMC.instance, task);
  }

  // schedule the task to run on a background thread
  public static void asynchronously(final Runnable task) {
    Bukkit.getScheduler().runTaskAsynchronously(ClodMC.instance, task);
  }

  // on the next game tick
  public static void nextTick(final Runnable task) {
    new BukkitRunnable() {
      @Override
      public void run() {
        task.run();
      }
    }.runTask(ClodMC.instance);
  }

  // once after waiting 'delay' ticks
  public static void delayed(final long delay, final Runnable task) {
    new BukkitRunnable() {
      @Override
      public void run() {
        task.run();
      }
    }.runTaskLater(ClodMC.instance, delay);
  }

  // periodically, every 'period' ticks
  public static BukkitTask periodically(final long period, final Runnable task) {
    return periodically(0, period, task);
  }

  // periodically, wait 'delay' ticks, then every 'period' ticks
  public static BukkitTask periodically(final long delay, final long period, final Runnable task) {
    return Bukkit.getScheduler().runTaskTimer(ClodMC.instance, task, delay, period);
  }
}
