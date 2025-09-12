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

/** displays griefprevention claims as markers on bluemap */
@NullMarked
public class BlueMapGriefPrevention extends Addon implements Listener {
  private static final Color ADMIN_LINE = new Color("#fd6600ff");
  private static final Color ADMIN_FILL = new Color("#fd660096");
  private static final Color PLAYER_LINE = new Color("#0060ffff");
  private static final Color PLAYER_FILL = new Color("#0087ff96");

  private final Map<World, MarkerSet> markerSets = new HashMap<>(3);

  public BlueMapGriefPrevention(final BlueMapAPI api) {
    super(api);

    for (final World world : Bukkit.getWorlds()) {
      this.markerSets.put(world, MarkerSet.builder().label("Claims").defaultHidden(true).build());
    }

    Bukkit.getPluginManager().registerEvents(this, ClodMC.instance);
  }

  // update claim markers on the bluemap
  @Override
  protected void update() {
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
      api.getWorld(entry.getKey())
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

  // refresh markers when a claim is created
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onClaimCreated(final ClaimCreatedEvent event) {
    this.update();
  }

  // refresh markers when a claim is resized
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onClaimResize(final ClaimResizeEvent event) {
    this.update();
  }

  // refresh markers when a claim is changed
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onClaimChange(final ClaimChangeEvent event) {
    this.update();
  }

  // refresh markers when a claim is extended
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onClaimExtend(final ClaimExtendEvent event) {
    this.update();
  }

  // refresh markers when a claim is deleted
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onClaimDeleted(final ClaimDeletedEvent event) {
    this.update();
  }
}
