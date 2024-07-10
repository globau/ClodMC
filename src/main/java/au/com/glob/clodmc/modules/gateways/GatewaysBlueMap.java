package au.com.glob.clodmc.modules.gateways;

import au.com.glob.clodmc.util.BlueMap;
import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public class GatewaysBlueMap {
  private final @NotNull Map<World, MarkerSet> markerSets = new HashMap<>(3);

  public GatewaysBlueMap(@NotNull Gateways gateways) {
    for (World world : Bukkit.getWorlds()) {
      this.markerSets.put(world, MarkerSet.builder().label("Gateways").defaultHidden(true).build());
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
                  .styleClasses("gw-anchor-block")
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
