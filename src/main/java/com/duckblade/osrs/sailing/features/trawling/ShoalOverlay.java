package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.BoatTracker;
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
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Singleton
public class ShoalOverlay extends Overlay
        implements PluginLifecycleComponent {

    private static final int SHOAL_HIGHLIGHT_SIZE = 10;


    


    // Clickbox IDs
    private static final Set<Integer> SHOAL_CLICKBOX_IDS = ImmutableSet.of(
            TrawlingData.ShoalObjectID.BLUEFIN,
            TrawlingData.ShoalObjectID.GIANT_KRILL,
            TrawlingData.ShoalObjectID.GLISTENING,
            TrawlingData.ShoalObjectID.HADDOCK,
            TrawlingData.ShoalObjectID.HALIBUT,
            TrawlingData.ShoalObjectID.MARLIN,
            TrawlingData.ShoalObjectID.SHIMMERING,
            TrawlingData.ShoalObjectID.VIBRANT,
            TrawlingData.ShoalObjectID.YELLOWFIN);

    @Nonnull
    private final Client client;
    private final SailingConfig config;
    private final ShoalDepthTracker shoalDepthTracker;
    private final BoatTracker boatTracker;
    private final Set<GameObject> shoals = new HashSet<>();

    @Inject
    public ShoalOverlay(@Nonnull Client client, SailingConfig config, 
                       ShoalDepthTracker shoalDepthTracker, BoatTracker boatTracker) {
        this.client = client;
        this.config = config;
        this.shoalDepthTracker = shoalDepthTracker;
        this.boatTracker = boatTracker;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(PRIORITY_HIGH);
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
        if (SHOAL_CLICKBOX_IDS.contains(objectId)) {
            shoals.add(obj);
            log.debug("Shoal spawned with ID {} at {} (total shoals: {})", objectId, obj.getLocalLocation(), shoals.size());
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



    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.trawlingHighlightShoals() || shoals.isEmpty()) {
            return null;
        }

        // Only highlight one shoal at a time - choose the first available shoal
        GameObject shoalToHighlight = selectShoalToHighlight();
        if (shoalToHighlight != null) {
            renderShoalHighlight(graphics, shoalToHighlight);
        }

        return null;
    }

    /**
     * Select which shoal to highlight when multiple shoals are present.
     * Priority: Special shoals (green) > Regular shoals (config color)
     */
    private GameObject selectShoalToHighlight() {
        GameObject firstSpecialShoal = null;
        GameObject firstRegularShoal = null;
        
        for (GameObject shoal : shoals) {
            if (isSpecialShoal(shoal.getId())) {
                if (firstSpecialShoal == null) {
                    firstSpecialShoal = shoal;
                }
            } else {
                if (firstRegularShoal == null) {
                    firstRegularShoal = shoal;
                }
            }
            
            // If we have both types, we can stop looking
            if (firstSpecialShoal != null && firstRegularShoal != null) {
                break;
            }
        }
        
        // Prioritize special shoals over regular ones
        return firstSpecialShoal != null ? firstSpecialShoal : firstRegularShoal;
    }

    private void renderShoalHighlight(Graphics2D graphics, GameObject shoal) {
        Polygon poly = Perspective.getCanvasTileAreaPoly(client, shoal.getLocalLocation(), SHOAL_HIGHLIGHT_SIZE);
        if (poly != null) {
            Color color = getShoalColor(shoal.getId());
            Stroke originalStroke = graphics.getStroke();
            graphics.setStroke(new BasicStroke(0.5f));
            OverlayUtil.renderPolygon(graphics, poly, color);
            graphics.setStroke(originalStroke);
        }
    }

    private Color getShoalColor(int objectId) {
        // Priority 1: Check depth matching (highest priority) - DISABLED
        // NetDepth shoalDepth = shoalDepthTracker.getCurrentDepth();
        // if (shoalDepth != null) {
        //     NetDepth playerDepth = getPlayerNetDepth();
        //     if (playerDepth != null && playerDepth != shoalDepth) {
        //         return Color.RED; // Wrong depth - highest priority
        //     }
        // }
        
        // Priority 2: Special shoals use green (medium priority)
        if (isSpecialShoal(objectId)) {
            return Color.GREEN;
        }
        
        // Priority 3: Default to configured color (lowest priority)
        return config.trawlingShoalHighlightColour();
    }





    /**
     * Check if the shoal is a special type (VIBRANT, GLISTENING, SHIMMERING)
     */
    private boolean isSpecialShoal(int objectId) {
        return objectId == TrawlingData.ShoalObjectID.VIBRANT ||
               objectId == TrawlingData.ShoalObjectID.GLISTENING ||
               objectId == TrawlingData.ShoalObjectID.SHIMMERING;
    }

}
