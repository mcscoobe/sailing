package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.WorldEntity;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WorldEntitySpawned;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.util.Set;

@Slf4j
@Singleton
public class NetDepthTimer extends Overlay implements PluginLifecycleComponent {

    // WorldEntity config ID for moving shoals
    private static final int SHOAL_WORLD_ENTITY_CONFIG_ID = 4;
    
    // Number of ticks shoal must be moving before we consider it "was moving"
    private static final int MOVEMENT_THRESHOLD_TICKS = 5;
    
    // Number of ticks at same position to consider shoal "stopped"
    private static final int STOPPED_THRESHOLD_TICKS = 2;
    
    // Shoal object IDs - used to detect any shoal presence
    private static final Set<Integer> SHOAL_OBJECT_IDS = ImmutableSet.of(
        TrawlingData.ShoalObjectID.MARLIN,
        TrawlingData.ShoalObjectID.BLUEFIN,
        TrawlingData.ShoalObjectID.VIBRANT,
        TrawlingData.ShoalObjectID.HALIBUT,
        TrawlingData.ShoalObjectID.GLISTENING,
        TrawlingData.ShoalObjectID.YELLOWFIN
    );

    private final Client client;
    private final SailingConfig config;

    // Movement tracking
    private WorldEntity movingShoal = null;
    private WorldPoint lastShoalPosition = null;
    private int ticksAtSamePosition = 0;
    private int ticksMoving = 0;
    private boolean hasBeenMoving = false;
    
    // Timer state
    private int shoalDuration = 0;
    private int timerTicks = 0;
    private boolean timerActive = false;

    @Inject
    public NetDepthTimer(Client client, SailingConfig config) {
        this.client = client;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(1000.0f);
    }

    @Override
    public boolean isEnabled(SailingConfig config) {
        return config.trawlingShowNetDepthTimer();
    }

    @Override
    public void startUp() {
        log.debug("NetDepthTimer started");
    }

    @Override
    public void shutDown() {
        log.debug("NetDepthTimer shut down");
        resetState();
    }

    /**
     * Get current timer information for display in overlay
     */
    public TimerInfo getTimerInfo() {
        if (movingShoal == null) {
            return null;
        }
        
        boolean shoalIsMoving = ticksAtSamePosition < STOPPED_THRESHOLD_TICKS;
        
        if (!timerActive) {
            if (shoalIsMoving) {
                return new TimerInfo(false, true, 0); // Waiting for shoal to stop
            } else {
                return new TimerInfo(false, false, 0); // Calibrating
            }
        }
        
        // Timer counts down to depth change (half duration)
        int depthChangeTime = shoalDuration / 2;
        int ticksUntilDepthChange = depthChangeTime - timerTicks;
        return new TimerInfo(true, false, Math.max(0, ticksUntilDepthChange));
    }

    @Subscribe
    public void onWorldEntitySpawned(WorldEntitySpawned e) {
        WorldEntity entity = e.getWorldEntity();
        
        // Only track shoal WorldEntity
        if (entity.getConfig() != null && entity.getConfig().getId() == SHOAL_WORLD_ENTITY_CONFIG_ID) {
            boolean hadExistingShoal = movingShoal != null;
            movingShoal = entity;
            
            // Only reset movement tracking if this is a completely new shoal
            if (!hadExistingShoal) {
                resetMovementTracking();
                log.debug("New shoal WorldEntity spawned - resetting movement tracking");
            } else {
                log.debug("Shoal WorldEntity updated (type change) - preserving movement state");
            }
            
            // Get shoal duration from location
            LocalPoint localPos = entity.getCameraFocus();
            if (localPos != null) {
                WorldPoint worldPos = WorldPoint.fromLocal(client, localPos);
                shoalDuration = TrawlingData.FishingAreas.getStopDurationForLocation(worldPos);
                log.debug("Shoal WorldEntity at {}, duration: {} ticks, timer active: {}", 
                         worldPos, shoalDuration, timerActive);
            }
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned e) {
        GameObject obj = e.getGameObject();
        if (SHOAL_OBJECT_IDS.contains(obj.getId())) {
            log.debug("Shoal GameObject spawned (ID={}) - timer active: {}, movingShoal exists: {}", 
                     obj.getId(), timerActive, movingShoal != null);
            // Don't reset state - this might be a shoal type change
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned e) {
        GameObject obj = e.getGameObject();
        if (SHOAL_OBJECT_IDS.contains(obj.getId())) {
            log.debug("Shoal GameObject despawned (ID={})", obj.getId());
            // Don't reset state immediately - shoal might just be changing type
            // Only reset if WorldEntity is also gone (checked in onGameTick)
        }
    }

    @Subscribe
    public void onGameTick(GameTick e) {
        if (movingShoal == null) {
            return;
        }
        
        // Check if WorldEntity is still valid
        if (movingShoal.getCameraFocus() == null) {
            // Try to find the shoal entity again
            findShoalEntity();
            if (movingShoal == null) {
                // WorldEntity is truly gone - reset state
                log.debug("WorldEntity no longer exists - resetting timer state");
                resetState();
                return;
            }
        }
        
        // Track movement
        LocalPoint localPos = movingShoal.getCameraFocus();
        if (localPos != null) {
            WorldPoint currentPos = WorldPoint.fromLocal(client, localPos);
            if (currentPos != null) {
                trackMovement(currentPos);
            }
        }
        
        // Update timer if active
        if (timerActive) {
            timerTicks++;
            int depthChangeTime = shoalDuration / 2;
            if (timerTicks >= depthChangeTime) {
                // Depth change reached - stop timer
                timerActive = false;
                log.debug("Depth change reached at {} ticks (half of {} total duration)", timerTicks, shoalDuration);
            }
        }
    }

    private void trackMovement(WorldPoint currentPos) {
        if (currentPos.equals(lastShoalPosition)) {
            // Shoal is stationary
            ticksAtSamePosition++;
            ticksMoving = 0;
            
            // Check if shoal just stopped after being in motion
            if (ticksAtSamePosition == STOPPED_THRESHOLD_TICKS && hasBeenMoving) {
                startTimer();
            }
        } else {
            // Shoal is moving
            lastShoalPosition = currentPos;
            ticksAtSamePosition = 0;
            ticksMoving++;
            
            // Mark as having been moving if it's moved for enough ticks
            if (ticksMoving >= MOVEMENT_THRESHOLD_TICKS) {
                hasBeenMoving = true;
            }
            
            // Stop timer if shoal starts moving again
            if (timerActive) {
                timerActive = false;
                log.debug("Timer stopped - shoal started moving");
            }
        }
    }

    private void startTimer() {
        if (shoalDuration > 0) {
            timerActive = true;
            timerTicks = 0;
            log.debug("Timer started - shoal stopped after movement (duration: {} ticks)", shoalDuration);
        }
    }

    private void findShoalEntity() {
        if (client.getTopLevelWorldView() != null) {
            for (WorldEntity entity : client.getTopLevelWorldView().worldEntities()) {
                if (entity.getConfig() != null && entity.getConfig().getId() == SHOAL_WORLD_ENTITY_CONFIG_ID) {
                    movingShoal = entity;
                    log.debug("Found shoal WorldEntity in scene");
                    break;
                }
            }
        }
    }

    private void resetMovementTracking() {
        lastShoalPosition = null;
        ticksAtSamePosition = 0;
        ticksMoving = 0;
        hasBeenMoving = false;
        timerActive = false;
        timerTicks = 0;
    }

    private void resetState() {
        movingShoal = null;
        shoalDuration = 0;
        resetMovementTracking();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        // Timer display is handled by TrawlingOverlay
        return null;
    }

    /**
     * Data class for exposing timer information to overlay
     */
    public static class TimerInfo {
        private final boolean active;
        private final boolean waiting;
        private final int ticksRemaining;

        public TimerInfo(boolean active, boolean waiting, int ticksRemaining) {
            this.active = active;
            this.waiting = waiting;
            this.ticksRemaining = ticksRemaining;
        }

        public boolean isActive() {
            return active;
        }

        public boolean isWaiting() {
            return waiting;
        }

        public int getTicksUntilDepthChange() {
            return ticksRemaining;
        }
    }
}