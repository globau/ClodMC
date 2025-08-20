package au.com.glob.clodmc.build;

@SuppressWarnings("NullabilityAnnotations")
public interface ThrowingRunnable extends Runnable {
  @Override
  default void run() {
    try {
      this.tryRun();
    } catch (final Throwable t) {
      throwUnchecked(t);
    }
  }

  @SuppressWarnings("unchecked")
  private static <E extends Throwable> void throwUnchecked(Throwable t) throws E {
    throw (E) t;
  }

  void tryRun() throws Throwable;
}
