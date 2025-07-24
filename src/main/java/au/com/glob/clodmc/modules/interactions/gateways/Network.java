package au.com.glob.clodmc.modules.interactions.gateways;

import org.jspecify.annotations.NullMarked;

@NullMarked
class Network {
  final Colour top;
  final Colour bottom;

  Network(int networkId) {
    Colour topColour = Colours.of((networkId >> 4) & 0x0F);
    Colour bottomColour = Colours.of(networkId & 0x0F);
    if (topColour == null || bottomColour == null) {
      throw new RuntimeException("malformed anchor networkID: " + networkId);
    }
    this.top = topColour;
    this.bottom = bottomColour;
  }
}
