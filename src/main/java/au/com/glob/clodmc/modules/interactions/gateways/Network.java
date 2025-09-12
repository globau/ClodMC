package au.com.glob.clodmc.modules.interactions.gateways;

import org.jspecify.annotations.NullMarked;

/** represents a gateway network identified by top and bottom colours */
@NullMarked
class Network {
  final Colour top;
  final Colour bottom;

  Network(final int networkId) {
    final Colour topColour = Colours.of((networkId >> 4) & 0x0F);
    final Colour bottomColour = Colours.of(networkId & 0x0F);
    if (topColour == null || bottomColour == null) {
      throw new RuntimeException("malformed anchor networkID: %d".formatted(networkId));
    }
    this.top = topColour;
    this.bottom = bottomColour;
  }

  // creates network instance from network id
  static Network of(final int networkId) {
    return new Network(networkId);
  }

  // converts colour pair to network id
  static int coloursToNetworkId(final Colour topColour, final Colour bottomColour) {
    return (topColour.index << 4) | bottomColour.index;
  }
}
