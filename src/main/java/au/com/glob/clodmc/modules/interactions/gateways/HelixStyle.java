package au.com.glob.clodmc.modules.interactions.gateways;

import org.bukkit.entity.Display;
import org.jspecify.annotations.NullMarked;

/** appearance of a gateway helix; speed is in radians per tick */
@NullMarked
record HelixStyle(int strands, double speed, Display.Brightness brightness) {
  static final HelixStyle ACTIVE = new HelixStyle(2, 0.075, new Display.Brightness(15, 15));
  static final HelixStyle INACTIVE = new HelixStyle(1, 0.05, new Display.Brightness(8, 8));
}
