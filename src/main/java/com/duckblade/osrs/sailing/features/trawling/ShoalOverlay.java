package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WorldViewUnloaded;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Singleton
public class ShoalOverlay extends Overlay
        implements PluginLifecycleComponent {

    private static final int SHOAL_HIGHLIGHT_SIZE = 9;

    // Clickbox IDs
    private static final Set<Integer> SHOAL_CLICKBOX_IDS = ImmutableSet.of(
            ObjectID.SAILING_SHOAL_CLICKBOX_BLUEFIN,
            ObjectID.SAILING_SHOAL_CLICKBOX_GIANT_KRILL,
            ObjectID.SAILING_SHOAL_CLICKBOX_GLISTENING,
            ObjectID.SAILING_SHOAL_CLICKBOX_HADDOCK,
            ObjectID.SAILING_SHOAL_CLICKBOX_HALIBUT,
            ObjectID.SAILING_SHOAL_CLICKBOX_MARLIN,
            ObjectID.SAILING_SHOAL_CLICKBOX_SHIMMERING,
            ObjectID.SAILING_SHOAL_CLICKBOX_VIBRANT,
            ObjectID.SAILING_SHOAL_CLICKBOX_YELLOWFIN);
    
    // Actual shoal object IDs (visible objects, not clickboxes)
    private static final Set<Integer> SHOAL_OBJECT_IDS = ImmutableSet.of(
            59736,  // Yellowfin shoal
            59737,  // Bluefin shoal
            59738,  // Haddock shoal
            59739,  // Halibut shoal
            59740,  // Marlin shoal
            59741,  // Giant krill shoal
            59742,  // Glistening shoal
            59743,  // Shimmering shoal
            59744   // Vibrant shoal
    );

    private final Client client;
    private final SailingConfig config;
    private final Set<GameObject> shoals = new HashSet<>();
    
    // Route tracking - track by object ID since shoals of same type follow same route
    private final Map<Integer, List<WorldPoint>> recordedRoutes = new HashMap<>();

    @Inject
    public ShoalOverlay(Client client, SailingConfig config) {
        this.client = client;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public boolean isEnabled(SailingConfig config) {
        // Enable if either highlighting or recording is enabled
        return config.trawlingHighlightShoals() || config.trawlingRecordShoalRoutes();
    }

    @Override
    public void startUp() {
        log.debug("ShoalOverlay starting up");
    }

    @Override
    public void shutDown() {
        log.debug("ShoalOverlay shutting down");
        if (config.trawlingRecordShoalRoutes() && !recordedRoutes.isEmpty()) {
            exportRecordedRoutes();
        }
        shoals.clear();
        recordedRoutes.clear();
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned e) {
        GameObject obj = e.getGameObject();
        int objectId = obj.getId();
        if (SHOAL_CLICKBOX_IDS.contains(objectId) || SHOAL_OBJECT_IDS.contains(objectId)) {
            shoals.add(obj);
            if (config.trawlingRecordShoalRoutes()) {
                log.info("Shoal spawned with ID {} at {} (total shoals: {})", objectId, obj.getWorldLocation(), shoals.size());
            } else {
                log.debug("Shoal spawned with ID {} at {} (total shoals: {})", objectId, obj.getLocalLocation(), shoals.size());
            }
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned e) {
        GameObject obj = e.getGameObject();
        if (shoals.remove(obj)) {
            if (config.trawlingRecordShoalRoutes()) {
                log.info("Shoal despawned with ID {} at {} (remaining shoals: {})", obj.getId(), obj.getWorldLocation(), shoals.size());
            } else {
                log.debug("Shoal despawned with ID {}", obj.getId());
            }
        }
    }

    @Subscribe
    public void onWorldViewUnloaded(WorldViewUnloaded e) {
        // Only clear shoals when we're not actively sailing
        // During sailing, shoals move and respawn frequently, so we keep them tracked
        if (!e.getWorldView().isTopLevel()) {
            return;
        }
        
        // Check if player and worldview are valid before calling isSailing
        if (client.getLocalPlayer() == null || client.getLocalPlayer().getWorldView() == null) {
            log.debug("Top-level world view unloaded (player/worldview null), clearing {} shoals", shoals.size());
            shoals.clear();
            return;
        }
        
        if (!SailingUtil.isSailing(client)) {
            log.debug("Top-level world view unloaded while not sailing, clearing {} shoals", shoals.size());
            shoals.clear();
        }
    }

    @Subscribe
    public void onGameTick(GameTick e) {
        if (!config.trawlingRecordShoalRoutes()) {
            return;
        }

        // Check if sailing with null safety
        if (client.getLocalPlayer() == null || client.getLocalPlayer().getWorldView() == null) {
            log.info("GameTick: Player or WorldView is null, skipping");
            return;
        }
        
        boolean isSailing = SailingUtil.isSailing(client);
        if (!isSailing) {
            log.info("GameTick: Not sailing, skipping");
            return;
        }

        log.info("GameTick: Recording routes for {} shoals", shoals.size());

        // Track positions of all active shoals
        for (GameObject shoal : shoals) {
            int objectId = shoal.getId();
            WorldPoint currentPos = shoal.getWorldLocation();
            
            // Initialize route list if needed
            List<WorldPoint> route = recordedRoutes.computeIfAbsent(objectId, k -> new ArrayList<>());
            
            // Avoid duplicate consecutive positions
            if (route.isEmpty() || !route.get(route.size() - 1).equals(currentPos)) {
                route.add(currentPos);
                log.info("Recorded position for shoal {}: {} (total waypoints: {})", 
                        objectId, currentPos, route.size());
            } else {
                log.info("Shoal {} still at same position: {}", objectId, currentPos);
            }
        }
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.trawlingHighlightShoals()) {
            return null;
        }

        if (shoals.isEmpty()) {
            return null;
        }

        for (GameObject shoal : shoals) {
            renderShoal(graphics, shoal);
        }

        return null;
    }

    private void renderShoal(Graphics2D graphics, GameObject shoal) {
        Polygon poly = Perspective.getCanvasTileAreaPoly(client, shoal.getLocalLocation(), SHOAL_HIGHLIGHT_SIZE);
        if (poly != null) {
            Color color = config.trawlingShoalHighlightColour();
            OverlayUtil.renderPolygon(graphics, poly, color);
        }
    }

    private void exportRecordedRoutes() {
        log.info("=== RECORDED SHOAL ROUTES ===");
        for (Map.Entry<Integer, List<WorldPoint>> entry : recordedRoutes.entrySet()) {
            int objectId = entry.getKey();
            List<WorldPoint> route = entry.getValue();
            
            if (route.isEmpty()) {
                continue;
            }
            
            log.info("Shoal ID {}: {} waypoints", objectId, route.size());
            log.info("SHOAL_ROUTES.put({}, Arrays.asList(", objectId);
            
            for (int i = 0; i < route.size(); i++) {
                WorldPoint wp = route.get(i);
                String comma = (i < route.size() - 1) ? "," : "";
                log.info("    new WorldPoint({}, {}, {}){}", wp.getX(), wp.getY(), wp.getPlane(), comma);
            }
            
            log.info("));");
            log.info("");
        }
        log.info("=== END RECORDED ROUTES ===");
    }
}
