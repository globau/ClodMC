package au.com.glob.clodmc.util;

import au.com.glob.clodmc.ClodMC;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class Schedule {
  public static void asynchronously(Runnable task) {
    Bukkit.getScheduler().runTaskAsynchronously(ClodMC.instance, task);
  }

  public static void nextTick(Runnable task) {
    new BukkitRunnable() {
      @Override
      public void run() {
        task.run();
      }
    }.runTask(ClodMC.instance);
  }

  public static void delayed(long delay, Runnable task) {
    new BukkitRunnable() {
      @Override
      public void run() {
        task.run();
      }
    }.runTaskLater(ClodMC.instance, delay);
  }

  public static BukkitTask periodically(long period, Runnable task) {
    return periodically(0, period, task);
  }

  public static BukkitTask periodically(long delay, long period, Runnable task) {
    return Bukkit.getScheduler().runTaskTimer(ClodMC.instance, task, delay, period);
  }

  public static void onMainThread(Runnable task) {
    Bukkit.getScheduler().scheduleSyncDelayedTask(ClodMC.instance, task);
  }

  public static void onMainThreadPeriodically(long delay, long period, Runnable task) {
    Bukkit.getScheduler().scheduleSyncRepeatingTask(ClodMC.instance, task, delay, period);
  }
}
