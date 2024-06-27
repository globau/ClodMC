package au.com.glob.clodmc.modules;

public interface Module {
  default boolean forceDisable() {
    return false;
  }
}
