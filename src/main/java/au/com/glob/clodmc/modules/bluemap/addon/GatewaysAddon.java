package au.com.glob.clodmc.modules.bluemap.addon;

import au.com.glob.clodmc.ClodMC;
import au.com.glob.clodmc.modules.bluemap.BlueMapAddon;
import au.com.glob.clodmc.modules.interactions.Gateways;
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
import org.jetbrains.annotations.NotNull;

public class GatewaysAddon extends BlueMapAddon {
  private static final @NotNull String MARKER_FILENAME = "gateway.svg";

  private final @NotNull Map<World, MarkerSet> markerSets = new HashMap<>(3);
  private final @NotNull Gateways module;

  public GatewaysAddon(@NotNull Gateways module) {
    super(Gateways.class);
    this.module = module;

    // create markers
    for (World world : Bukkit.getWorlds()) {
      this.markerSets.put(
          world, MarkerSet.builder().label("Gateways").defaultHidden(false).build());
    }
  }

  @Override
  protected void onEnable(@NotNull BlueMapAPI api) {
    // create svg
    Path gatewayFilePath = api.getWebApp().getWebRoot().resolve("assets").resolve(MARKER_FILENAME);
    try {
      Files.createDirectories(gatewayFilePath.getParent());
      try (OutputStream out = Files.newOutputStream(gatewayFilePath)) {
        InputStream svgStream = ClodMC.instance.getResource(MARKER_FILENAME);
        Objects.requireNonNull(svgStream).transferTo(out);
      }
    } catch (IOException e) {
      Logger.error("failed to create " + gatewayFilePath + ": " + e);
    }
  }

  @Override
  public void onUpdate(@NotNull BlueMapAPI api) {
    for (MarkerSet markerSet : this.markerSets.values()) {
      markerSet.getMarkers().clear();
    }

    Set<String> seenColours = new HashSet<>();
    for (Gateways.AnchorBlock anchorBlock : this.module.getAnchorBlocks()) {
      if (anchorBlock.getName() == null) {
        continue;
      }

      String id =
          "gw-"
              + anchorBlock.getTopColour().getName()
              + "-"
              + anchorBlock.getBottomColour().getName()
              + "-";
      id = id + (seenColours.contains(id) ? "b" : "a");
      seenColours.add(id);

      Objects.requireNonNull(this.markerSets.get(anchorBlock.getBlockPos().getWorld()))
          .getMarkers()
          .put(
              id,
              POIMarker.builder()
                  .label(
                      anchorBlock.getName()
                          + "\n"
                          + anchorBlock.getBlockPos().getX()
                          + ", "
                          + anchorBlock.getBlockPos().getZ())
                  .position(
                      Vector3d.from(
                          anchorBlock.getBlockPos().getX() + 0.5,
                          anchorBlock.getBlockPos().getY() + 0.5,
                          anchorBlock.getBlockPos().getZ() + 0.5))
                  .icon("assets/" + MARKER_FILENAME, new Vector2i(25, 45))
                  .build());
    }

    for (Map.Entry<World, MarkerSet> entry : this.markerSets.entrySet()) {
      String mapId = "gw-" + entry.getKey().getName();
      api.getWorld(entry.getKey())
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
