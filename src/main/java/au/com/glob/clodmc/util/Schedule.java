package au.com.glob.clodmc.util;

import au.com.glob.clodmc.ClodMC;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.NullMarked;

/** wrappers around Bukkit scheduling, with clearer and cleaner semantics */
@NullMarked
public class Schedule {
  // schedule the task to run on the main thread
  public static void onMainThread(Runnable task) {
    Bukkit.getScheduler().scheduleSyncDelayedTask(ClodMC.instance, task);
  }

  // schedule the task to run on a background thread
  public static void asynchronously(Runnable task) {
    Bukkit.getScheduler().runTaskAsynchronously(ClodMC.instance, task);
  }

  // on the next game tick
  public static void nextTick(Runnable task) {
    new BukkitRunnable() {
      @Override
      public void run() {
        task.run();
      }
    }.runTask(ClodMC.instance);
  }

  // once after waiting 'delay' ticks
  public static void delayed(long delay, Runnable task) {
    new BukkitRunnable() {
      @Override
      public void run() {
        task.run();
      }
    }.runTaskLater(ClodMC.instance, delay);
  }

  // periodically, every 'period' ticks
  public static BukkitTask periodically(long period, Runnable task) {
    return periodically(0, period, task);
  }

  // periodically, wait 'delay' ticks, then every 'period' ticks
  public static BukkitTask periodically(long delay, long period, Runnable task) {
    return Bukkit.getScheduler().runTaskTimer(ClodMC.instance, task, delay, period);
  }
}
