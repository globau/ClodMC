package au.com.glob.clodmc.modules.gateways;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.util.BlueMap;
import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3d;
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
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public class GatewaysBlueMap {
  private static final @NotNull String MARKER_FILENAME = "gateway.svg";

  private final @NotNull Map<World, MarkerSet> markerSets = new HashMap<>(3);

  public GatewaysBlueMap(@NotNull Gateways gateways) {
    assert BlueMap.api != null;

    // create svg
    Path gatewayFilePath =
        BlueMap.api.getWebApp().getWebRoot().resolve("assets").resolve(MARKER_FILENAME);
    try {
      Files.createDirectories(gatewayFilePath.getParent());
      try (OutputStream out = Files.newOutputStream(gatewayFilePath)) {
        InputStream svgStream = ClodMC.instance.getResource(MARKER_FILENAME);
        assert svgStream != null;
        svgStream.transferTo(out);
      }
    } catch (IOException e) {
      ClodMC.logError("failed to create " + gatewayFilePath + ": " + e);
    }

    // create markers
    for (World world : Bukkit.getWorlds()) {
      this.markerSets.put(
          world, MarkerSet.builder().label("Gateways").defaultHidden(false).build());
    }
    this.update(gateways);
  }

  public void update(@NotNull Gateways gateways) {
    assert BlueMap.api != null;

    for (MarkerSet markerSet : this.markerSets.values()) {
      markerSet.getMarkers().clear();
    }

    Set<String> seenColours = new HashSet<>();
    for (AnchorBlock anchorBlock : gateways.getAnchorBlocks()) {
      if (anchorBlock.name == null) {
        continue;
      }

      String id =
          "gw-"
              + Config.idToTopName(anchorBlock.networkId)
              + "-"
              + Config.idToBottomName(anchorBlock.networkId)
              + "-";
      id = id + (seenColours.contains(id) ? "b" : "a");
      seenColours.add(id);

      this.markerSets
          .get(anchorBlock.blockPos.getWorld())
          .getMarkers()
          .put(
              id,
              POIMarker.builder()
                  .label(
                      anchorBlock.name
                          + "\n"
                          + anchorBlock.blockPos.getX()
                          + ", "
                          + anchorBlock.blockPos.getZ())
                  .position(
                      Vector3d.from(
                          anchorBlock.blockPos.getX() + 0.5,
                          anchorBlock.blockPos.getY() + 0.5,
                          anchorBlock.blockPos.getZ() + 0.5))
                  .icon("assets/" + MARKER_FILENAME, new Vector2i(25, 45))
                  .build());
    }

    for (Map.Entry<World, MarkerSet> entry : this.markerSets.entrySet()) {
      String mapId = "gw-" + entry.getKey().getName();
      BlueMap.api
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
