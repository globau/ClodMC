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
import org.jspecify.annotations.NullMarked;

@NullMarked
public class BlueMapGriefPrevention extends Addon implements Listener {
  private static final Color ADMIN_LINE = new Color("#fd6600ff");
  private static final Color ADMIN_FILL = new Color("#fd660096");
  private static final Color PLAYER_LINE = new Color("#0060ffff");
  private static final Color PLAYER_FILL = new Color("#0087ff96");

  private final Map<World, MarkerSet> markerSets = new HashMap<>(3);

  public BlueMapGriefPrevention(BlueMapAPI api) {
    super(api);

    for (World world : Bukkit.getWorlds()) {
      this.markerSets.put(world, MarkerSet.builder().label("Claims").defaultHidden(true).build());
    }

    Bukkit.getPluginManager().registerEvents(this, ClodMC.instance);
  }

  @Override
  protected void update() {
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

      // positions are in the north-west corner of the block
      // need to extend the south-east corner to cover the whole claim area
      Shape shape =
          new Shape(
              new Vector2d(lesserCorner.getX(), lesserCorner.getZ()),
              new Vector2d(greaterCorner.getX() + 1, lesserCorner.getZ()),
              new Vector2d(greaterCorner.getX() + 1, greaterCorner.getZ() + 1),
              new Vector2d(lesserCorner.getX(), greaterCorner.getZ() + 1));

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
  public void onClaimCreated(ClaimCreatedEvent event) {
    this.update();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onClaimResize(ClaimResizeEvent event) {
    this.update();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onClaimChange(ClaimChangeEvent event) {
    this.update();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onClaimExtend(ClaimExtendEvent event) {
    this.update();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onClaimDeleted(ClaimDeletedEvent event) {
    this.update();
  }
}
