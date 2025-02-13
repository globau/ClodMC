package au.com.glob.clodmc.modules.bluemap;

import au.com.glob.clodmc.ClodMC;
import com.flowpowered.math.vector.Vector2d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.ExtrudeMarker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.events.ClaimChangeEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimCreatedEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimDeletedEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimExtendEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimResizeEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class BlueMapGriefPrevention extends BlueMap.Addon implements Listener {
  private static final @NotNull Color ADMIN_LINE = new Color("#fd6600ff");
  private static final @NotNull Color ADMIN_FILL = new Color("#fd660096");
  private static final @NotNull Color PLAYER_LINE = new Color("#0060ffff");
  private static final @NotNull Color PLAYER_FILL = new Color("#0087ff96");

  private final @NotNull Map<World, MarkerSet> markerSets = new HashMap<>(3);

  public BlueMapGriefPrevention(@NotNull BlueMapAPI api) {
    super(api);

    for (World world : Bukkit.getWorlds()) {
      this.markerSets.put(world, MarkerSet.builder().label("Claims").defaultHidden(true).build());
    }

    Bukkit.getPluginManager().registerEvents(this, ClodMC.instance);
  }

  @Override
  protected void update() {
    if (this.api == null) {
      return;
    }

    for (MarkerSet markerSet : this.markerSets.values()) {
      markerSet.getMarkers().clear();
    }

    for (Claim claim : GriefPrevention.instance.dataStore.getClaims()) {
      if (!claim.inDataStore) {
        continue;
      }

      World world = claim.getLesserBoundaryCorner().getWorld();
      Location lesserCorner = claim.getLesserBoundaryCorner();
      Location greaterCorner = claim.getGreaterBoundaryCorner();

      Shape shape =
          new Shape(
              new Vector2d(lesserCorner.getX(), lesserCorner.getZ()),
              new Vector2d(greaterCorner.getX(), lesserCorner.getZ()),
              new Vector2d(greaterCorner.getX(), greaterCorner.getZ()),
              new Vector2d(lesserCorner.getX(), greaterCorner.getZ()));

      Objects.requireNonNull(this.markerSets.get(world))
          .getMarkers()
          .put(
              "claim-" + claim.getID(),
              ExtrudeMarker.builder()
                  .position(
                      lesserCorner.getBlockX() + 0.0,
                      lesserCorner.getBlockY() + 0.0,
                      lesserCorner.getBlockZ() + 0.0)
                  .shape(shape, lesserCorner.getBlockY(), world.getMaxHeight())
                  .label(claim.isAdminClaim() ? "Admin Claim" : claim.getOwnerName() + "'s Claim")
                  .lineColor(claim.isAdminClaim() ? ADMIN_LINE : PLAYER_LINE)
                  .fillColor(claim.isAdminClaim() ? ADMIN_FILL : PLAYER_FILL)
                  .build());
    }

    for (Map.Entry<World, MarkerSet> entry : this.markerSets.entrySet()) {
      String mapId = "claim-" + entry.getKey().getName();
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

  @EventHandler(priority = EventPriority.MONITOR)
  public void onClaimCreated(@NotNull ClaimCreatedEvent event) {
    this.update();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onClaimResize(@NotNull ClaimResizeEvent event) {
    this.update();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onClaimChange(@NotNull ClaimChangeEvent event) {
    this.update();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onClaimExtend(@NotNull ClaimExtendEvent event) {
    this.update();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onClaimDelete(@NotNull ClaimDeletedEvent event) {
    this.update();
  }
}
