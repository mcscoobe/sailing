package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
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
import java.util.HashSet;
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

    @Inject
    public ShoalOverlay(Client client, SailingConfig config) {
        this.client = client;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public boolean isEnabled(SailingConfig config) {
        return config.trawlingHighlightShoals();
    }

    @Override
    public void startUp() {
        log.debug("ShoalOverlay starting up");
    }

    @Override
    public void shutDown() {
        log.debug("ShoalOverlay shutting down");
        shoals.clear();
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned e) {
        GameObject obj = e.getGameObject();
        int objectId = obj.getId();
        if (SHOAL_CLICKBOX_IDS.contains(objectId) || SHOAL_OBJECT_IDS.contains(objectId)) {
            log.debug("Shoal spawned with ID {} at {} (total shoals: {})", objectId, obj.getLocalLocation(), shoals.size() + 1);
            shoals.add(obj);
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned e) {
        GameObject obj = e.getGameObject();
        if (shoals.remove(obj)) {
            log.debug("Shoal despawned with ID {}", obj.getId());
        }
    }

    @Subscribe
    public void onWorldViewUnloaded(WorldViewUnloaded e) {
        // Only clear shoals when we're not actively sailing
        // During sailing, shoals move and respawn frequently, so we keep them tracked
        if (e.getWorldView().isTopLevel() && !SailingUtil.isSailing(client)) {
            log.debug("Top-level world view unloaded while not sailing, clearing {} shoals", shoals.size());
            shoals.clear();
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

        log.debug("Rendering {} shoals", shoals.size());
        for (GameObject shoal : shoals) {
            renderShoal(graphics, shoal);
        }

        return null;
    }

    private void renderShoal(Graphics2D graphics, GameObject shoal) {
        Polygon poly = Perspective.getCanvasTileAreaPoly(client, shoal.getLocalLocation(), SHOAL_HIGHLIGHT_SIZE);
        if (poly != null) {
            Color color = config.trawlingShoalHighlightColour();
            log.debug("Drawing polygon for shoal at {} with color {}", shoal.getLocalLocation(), color);
            OverlayUtil.renderPolygon(graphics, poly, color);
        } else {
            log.debug("Polygon is null for shoal at {}", shoal.getLocalLocation());
        }
    }
}
