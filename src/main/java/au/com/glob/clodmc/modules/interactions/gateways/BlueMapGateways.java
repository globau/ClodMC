package au.com.glob.clodmc.modules.interactions.gateways;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.bluemap.Addon;
import au.com.glob.clodmc.util.Logger;
import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** displays gateway anchors as markers on bluemap */
@NullMarked
public class BlueMapGateways extends Addon {
  @Nullable static BlueMapGateways instance = null;

  private static final String MARKER_FILENAME = "gateway.svg";

  private final Map<World, MarkerSet> markerSets = new HashMap<>(3);

  // initialises bluemap integration for gateway markers
  public BlueMapGateways(BlueMapAPI api) {
    super(api);
    instance = this;

    // create svg
    Path gatewayFilePath = api.getWebApp().getWebRoot().resolve("assets").resolve(MARKER_FILENAME);
    try {
      Files.createDirectories(gatewayFilePath.getParent());
      try (OutputStream out = Files.newOutputStream(gatewayFilePath)) {
        InputStream svgStream = ClodMC.instance.getResource(MARKER_FILENAME);
        Objects.requireNonNull(svgStream).transferTo(out);
      }
    } catch (IOException e) {
      Logger.error("failed to create %s: %s".formatted(gatewayFilePath, e));
    }

    // create markers
    for (World world : Bukkit.getWorlds()) {
      this.markerSets.put(
          world, MarkerSet.builder().label("Gateways").defaultHidden(false).build());
    }
  }

  // updates gateway markers on bluemap
  @Override
  public void update() {
    for (MarkerSet markerSet : this.markerSets.values()) {
      markerSet.getMarkers().clear();
    }

    Set<String> seenColours = new HashSet<>();
    for (AnchorBlock anchorBlock : Gateways.instance.getAnchorBlocks()) {
      if (anchorBlock.name == null) {
        continue;
      }

      String id = "gw-%s-%s-".formatted(anchorBlock.topColour.name, anchorBlock.bottomColour.name);
      id = "%s%s".formatted(id, seenColours.contains(id) ? "b" : "a");
      seenColours.add(id);

      Objects.requireNonNull(this.markerSets.get(anchorBlock.blockPos.world))
          .getMarkers()
          .put(
              id,
              POIMarker.builder()
                  .label(
                      "%s\n%d, %d"
                          .formatted(
                              anchorBlock.name, anchorBlock.blockPos.x, anchorBlock.blockPos.z))
                  .position(
                      Vector3d.from(
                          anchorBlock.blockPos.x + 0.5,
                          anchorBlock.blockPos.y + 0.5,
                          anchorBlock.blockPos.z + 0.5))
                  .icon("assets/%s".formatted(MARKER_FILENAME), new Vector2i(25, 45))
                  .build());
    }

    for (Map.Entry<World, MarkerSet> entry : this.markerSets.entrySet()) {
      String mapId = "gw-%s".formatted(entry.getKey().getName());
      this.api
          .getWorld(entry.getKey())
          .ifPresent(
              (BlueMapWorld world) -> {
                for (BlueMapMap map : world.getMaps()) {
                  if (entry.getValue().getMarkers().isEmpty()) {
                    map.getMarkerSets().remove(mapId);
                  } else {
                    map.getMarkerSets().put(mapId, entry.getValue());
                  }
                }
              });
    }
  }
}
