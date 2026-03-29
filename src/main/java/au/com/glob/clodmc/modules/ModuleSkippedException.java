package au.com.glob.clodmc.modules;

import org.jspecify.annotations.NullMarked;

/** thrown by Module constructor when a required plugin is not available */
@NullMarked
class ModuleSkippedException extends RuntimeException {
  ModuleSkippedException() {
    super(null, null, true, false);
  }
}
