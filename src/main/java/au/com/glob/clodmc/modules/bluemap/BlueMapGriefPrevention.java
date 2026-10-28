package au.com.glob.clodmc.modules.bluemap;

import au.com.glob.clodmc.events.BlueMapInitEvent;
import au.com.glob.clodmc.util.Schedule;
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
import me.ryanhamshire.GriefPrevention.events.ClaimTransferEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** displays griefprevention claims as markers on bluemap */
@NullMarked
public class BlueMapGriefPrevention implements Listener {
  private static final Color ADMIN_LINE = new Color("#fd6600ff");
  private static final Color ADMIN_FILL = new Color("#fd660096");
  private static final Color PLAYER_LINE = new Color("#0060ffff");
  private static final Color PLAYER_FILL = new Color("#0087ff96");

  private @Nullable BlueMapAPI api;
  private final Map<World, MarkerSet> markerSets = new HashMap<>(3);
  private boolean updatePending;

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlueMapInit(final BlueMapInitEvent event) {
    this.api = event.getApi();

    for (final World world : Bukkit.getWorlds()) {
      this.markerSets.put(world, MarkerSet.builder().label("Claims").defaultHidden(true).build());
    }

    this.update();
  }

  // update claim markers on the bluemap
  private void update() {
    if (this.api == null) {
      return;
    }

    for (final MarkerSet markerSet : this.markerSets.values()) {
      markerSet.getMarkers().clear();
    }

    for (final Claim claim : GriefPrevention.instance.dataStore.getClaims()) {
      if (!claim.inDataStore) {
        continue;
      }

      final World world = claim.getLesserBoundaryCorner().getWorld();
      final Location lesserCorner = claim.getLesserBoundaryCorner();
      final Location greaterCorner = claim.getGreaterBoundaryCorner();

      // positions are in the north-west corner of the block
      // need to extend the south-east corner to cover the whole claim area
      final Shape shape =
          new Shape(
              new Vector2d(lesserCorner.getX(), lesserCorner.getZ()),
              new Vector2d(greaterCorner.getX() + 1, lesserCorner.getZ()),
              new Vector2d(greaterCorner.getX() + 1, greaterCorner.getZ() + 1),
              new Vector2d(lesserCorner.getX(), greaterCorner.getZ() + 1));

      Objects.requireNonNull(this.markerSets.get(world))
          .getMarkers()
          .put(
              "claim-%d".formatted(claim.getID()),
              ExtrudeMarker.builder()
                  .position(
                      lesserCorner.getBlockX() + 0.0,
                      lesserCorner.getBlockY() + 0.0,
                      lesserCorner.getBlockZ() + 0.0)
                  .shape(shape, lesserCorner.getBlockY(), world.getMaxHeight())
                  .label(
                      claim.isAdminClaim()
                          ? "Admin Claim"
                          : "%s's Claim".formatted(claim.getOwnerName()))
                  .lineColor(claim.isAdminClaim() ? ADMIN_LINE : PLAYER_LINE)
                  .fillColor(claim.isAdminClaim() ? ADMIN_FILL : PLAYER_FILL)
                  .build());
    }

    for (final Map.Entry<World, MarkerSet> entry : this.markerSets.entrySet()) {
      final String mapId = "claim-%s".formatted(entry.getKey().getName());
      this.api
          .getWorld(entry.getKey())
          .ifPresent(
              (final BlueMapWorld world) -> {
                for (final BlueMapMap map : world.getMaps()) {
                  if (entry.getValue().getMarkers().isEmpty()) {
                    map.getMarkerSets().remove(mapId);
                  } else {
                    map.getMarkerSets().put(mapId, entry.getValue());
                  }
                }
              });
    }
  }

  // griefprevention fires claim events before applying the change, so update on the next tick;
  // multiple events within a tick result in a single update
  private void scheduleUpdate() {
    if (this.updatePending) {
      return;
    }
    this.updatePending = true;
    Schedule.nextTick(
        () -> {
          this.updatePending = false;
          this.update();
        });
  }

  // refresh markers when a claim is created
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onClaimCreated(final ClaimCreatedEvent event) {
    this.scheduleUpdate();
  }

  // refresh markers when a claim is resized or extended
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onClaimChange(final ClaimChangeEvent event) {
    this.scheduleUpdate();
  }

  // refresh markers when a claim changes owner
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onClaimTransfer(final ClaimTransferEvent event) {
    this.scheduleUpdate();
  }

  // refresh markers when a claim is deleted
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onClaimDeleted(final ClaimDeletedEvent event) {
    this.scheduleUpdate();
  }
}
