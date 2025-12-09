package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.model.Boat;
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
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Singleton
public class NetDepthTimer extends Overlay
        implements PluginLifecycleComponent {

    // WorldEntity config ID for moving shoals
    private static final int SHOAL_WORLD_ENTITY_CONFIG_ID = 4;
    
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


    
    // Depth transitions based on stop duration
    private static final Map<Integer, ShoalTiming> DURATION_TO_TIMING = new HashMap<>();
    
    static {
        // Marlin areas: 50 ticks, Moderate -> Deep
        DURATION_TO_TIMING.put(TrawlingData.ShoalStopDuration.MARLIN, 
            new ShoalTiming(TrawlingData.ShoalStopDuration.MARLIN, NetDepth.MODERATE, NetDepth.DEEP));
        
        // Bluefin areas: 66 ticks, Shallow -> Moderate
        DURATION_TO_TIMING.put(TrawlingData.ShoalStopDuration.BLUEFIN, 
            new ShoalTiming(TrawlingData.ShoalStopDuration.BLUEFIN, NetDepth.SHALLOW, NetDepth.MODERATE));
        
        // Halibut areas: 80 ticks, Shallow -> Moderate
        DURATION_TO_TIMING.put(TrawlingData.ShoalStopDuration.HALIBUT, 
            new ShoalTiming(TrawlingData.ShoalStopDuration.HALIBUT, NetDepth.SHALLOW, NetDepth.MODERATE));
        
        // Yellowfin areas: 100 ticks, Shallow -> Moderate
        DURATION_TO_TIMING.put(TrawlingData.ShoalStopDuration.YELLOWFIN, 
            new ShoalTiming(TrawlingData.ShoalStopDuration.YELLOWFIN, NetDepth.SHALLOW, NetDepth.MODERATE));
    }
    
    // Widget indices for fishing net controls
    private static final int STARBOARD_DOWN = 97;
    private static final int STARBOARD_UP = 108;
    private static final int PORT_DOWN = 132;
    private static final int PORT_UP = 143;
    
    // Widget indices for net depth indicators
    private static final int STARBOARD_DEPTH_WIDGET_INDEX = 96;
    private static final int PORT_DEPTH_WIDGET_INDEX = 131;
    
    // Sprite IDs for each depth level
    private static final int SPRITE_SHALLOW = 7081;
    private static final int SPRITE_MODERATE = 7082;
    private static final int SPRITE_DEEP = 7083;

    private final Client client;
    private final SailingConfig config;
    private final BoatTracker boatTracker;

    // Track WorldEntity (moving shoal) for position monitoring
    private WorldEntity movingShoal = null;
    private WorldPoint lastShoalPosition = null;
    private int ticksAtSamePosition = 0;
    private boolean hasSeenShoalStop = false;
    
    // Track the active shoal timer
    private ShoalTracker activeTracker = null;
    


    @Inject
    public NetDepthTimer(Client client, SailingConfig config, BoatTracker boatTracker) {
        this.client = client;
        this.config = config;
        this.boatTracker = boatTracker;
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
        movingShoal = null;
        lastShoalPosition = null;
        ticksAtSamePosition = 0;
        hasSeenShoalStop = false;
        activeTracker = null;
    }

    /**
     * Get current timer information for display in overlay
     * @return TimerInfo object with current state, or null if no shoal is being tracked
     */
    public TimerInfo getTimerInfo() {
        if (activeTracker == null) {
            return null;
        }
        TimerInfo info = activeTracker.getTimerInfo();
        return info;
    }

    @Subscribe
    public void onWorldEntitySpawned(WorldEntitySpawned e) {
        WorldEntity entity = e.getWorldEntity();
        
        // Log all WorldEntity spawns
        if (entity.getConfig() != null) {
            log.debug("WorldEntity spawned - Config ID: {}", entity.getConfig().getId());
        }
        
        // Only track shoal WorldEntity
        if (entity.getConfig() != null && entity.getConfig().getId() == SHOAL_WORLD_ENTITY_CONFIG_ID) {
            movingShoal = entity;
            lastShoalPosition = null;
            ticksAtSamePosition = 0;
            log.info("Shoal WorldEntity spawned, tracking movement");
            
            // Create tracker if we don't have one yet, using WorldEntity's position
            if (activeTracker == null) {
                LocalPoint localPos = entity.getCameraFocus();
                if (localPos != null) {
                    WorldPoint worldPos = WorldPoint.fromLocal(client, localPos);
                    int stopDuration = TrawlingData.FishingAreas.getStopDurationForLocation(worldPos);
                    
                    log.info("Shoal WorldEntity at location: {}, StopDuration: {}", worldPos, stopDuration);
                    
                    if (stopDuration > 0) {
                        activeTracker = new ShoalTracker(stopDuration, worldPos);
                        log.info("Created ShoalTracker at location {}: stop duration = {} ticks", 
                                 worldPos, stopDuration);
                    } else {
                        log.warn("Shoal spawned at unknown location: {} (not in any defined fishing area)", worldPos);
                    }
                }
            }
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned e) {
        GameObject obj = e.getGameObject();
        int objectId = obj.getId();
        
        if (SHOAL_OBJECT_IDS.contains(objectId)) {
            log.debug("Shoal GameObject detected (ID={}), waiting for WorldEntity to get proper coordinates", objectId);
            // Don't create tracker yet - wait for WorldEntity spawn to get proper top-level coordinates
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned e) {
        GameObject obj = e.getGameObject();
        int objectId = obj.getId();
        
        if (SHOAL_OBJECT_IDS.contains(objectId)) {
            // Shoal left world view - reset everything
            log.debug("Shoal despawned (left world view): ID={}", objectId);
            activeTracker = null;
            movingShoal = null;
            lastShoalPosition = null;
            ticksAtSamePosition = 0;
            hasSeenShoalStop = false;
        }
    }

    @Subscribe
    public void onGameTick(GameTick e) {
        // If we don't have a moving shoal but have an active tracker, try to find it
        if (movingShoal == null && activeTracker != null) {
            if (client.getTopLevelWorldView() != null) {
                for (WorldEntity entity : client.getTopLevelWorldView().worldEntities()) {
                    if (entity.getConfig() != null && entity.getConfig().getId() == SHOAL_WORLD_ENTITY_CONFIG_ID) {
                        movingShoal = entity;
                        lastShoalPosition = null;
                        ticksAtSamePosition = 0;
                        log.debug("Found shoal WorldEntity in scene, tracking movement");
                        break;
                    }
                }
            }
        }
        
        // Track WorldEntity movement to detect when it stops
        if (movingShoal != null && activeTracker != null) {
            net.runelite.api.coords.LocalPoint localPos = movingShoal.getCameraFocus();
            if (localPos != null) {
                WorldPoint currentPos = WorldPoint.fromLocal(client, localPos);
                if (currentPos != null) {
                    if (currentPos.equals(lastShoalPosition)) {
                        ticksAtSamePosition++;
                        log.debug("Shoal at same position: {} ticks", ticksAtSamePosition);
                        
                        if (ticksAtSamePosition == STOPPED_THRESHOLD_TICKS && !hasSeenShoalStop) {
                            // First time seeing shoal stop
                            hasSeenShoalStop = true;
                            log.info("Shoal stopped at {} (first stop observed, waiting for movement)", currentPos);
                        } else if (ticksAtSamePosition == STOPPED_THRESHOLD_TICKS && hasSeenShoalStop) {
                            // Shoal stopped again after moving - restart timer
                            activeTracker.restart();
                            log.info("Shoal stopped at {}, timer restarted", currentPos);
                        }
                    } else {
                        if (lastShoalPosition != null) {
                            log.info("Shoal moved from {} to {}", lastShoalPosition, currentPos);
                        }
                        lastShoalPosition = currentPos;
                        ticksAtSamePosition = 0;
                    }
                }
            }
        }
        
        // Update timer
        if (activeTracker != null) {
            activeTracker.tick();
        }
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.trawlingShowNetDepthTimer()) {
            return null;
        }

        Boat boat = boatTracker.getBoat();
        if (boat == null || boat.getNetTiers().isEmpty()) {
            return null;
        }

        Widget widgetSailingRows = client.getWidget(InterfaceID.SailingSidepanel.FACILITIES_ROWS);
        if (widgetSailingRows == null) {
            return null;
        }

        // Check if we have an active tracker and highlight buttons if needed
        if (activeTracker != null) {
            NetDepth requiredDepth = activeTracker.getCurrentRequiredDepth();
            if (requiredDepth != null) {
                highlightButtonsForDepth(graphics, widgetSailingRows, requiredDepth);
            }
        }

        return null;
    }

    private void highlightButtonsForDepth(Graphics2D graphics, Widget parent, NetDepth requiredDepth) {
        Color highlightColor = config.trawlingShoalHighlightColour();

        // Check starboard net - only highlight if opacity is 0 (player can interact)
        Widget starboardDepthWidget = parent.getChild(STARBOARD_DEPTH_WIDGET_INDEX);
        if (starboardDepthWidget != null && starboardDepthWidget.getOpacity() == 0) {
            NetDepth currentDepth = getNetDepth(parent, STARBOARD_DEPTH_WIDGET_INDEX);
            if (currentDepth != null && currentDepth != requiredDepth) {
                highlightNetButton(graphics, parent, currentDepth, requiredDepth, 
                                  STARBOARD_UP, STARBOARD_DOWN, highlightColor);
            }
        }

        // Check port net - only highlight if opacity is 0 (player can interact)
        Widget portDepthWidget = parent.getChild(PORT_DEPTH_WIDGET_INDEX);
        if (portDepthWidget != null && portDepthWidget.getOpacity() == 0) {
            NetDepth currentDepth = getNetDepth(parent, PORT_DEPTH_WIDGET_INDEX);
            if (currentDepth != null && currentDepth != requiredDepth) {
                highlightNetButton(graphics, parent, currentDepth, requiredDepth,
                                  PORT_UP, PORT_DOWN, highlightColor);
            }
        }
    }

    private void highlightNetButton(Graphics2D graphics, Widget parent, NetDepth current, 
                                    NetDepth required, int upIndex, int downIndex, Color color) {
        // Determine which button to highlight
        int buttonIndex;
        if (required.ordinal() < current.ordinal()) {
            // Need to go shallower (up)
            buttonIndex = upIndex;
        } else {
            // Need to go deeper (down)
            buttonIndex = downIndex;
        }

        Widget button = getNetWidget(parent, buttonIndex);
        if (button != null && !button.isHidden()) {
            Rectangle bounds = button.getBounds();
            if (bounds.width > 0 && bounds.height > 0) {
                // Check if button is actually visible in the viewport (not scrolled out of view)
                if (isWidgetInViewport(button, parent)) {
                    graphics.setColor(color);
                    graphics.setStroke(new BasicStroke(3));
                    graphics.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
                }
            }
        }
    }

    private boolean isWidgetInViewport(Widget widget, Widget scrollContainer) {
        if (widget == null || scrollContainer == null) {
            return false;
        }
        
        Rectangle widgetBounds = widget.getBounds();
        
        // Find the actual scroll viewport by looking for the parent with scroll properties
        Widget scrollViewport = scrollContainer;
        while (scrollViewport != null && scrollViewport.getScrollHeight() == 0) {
            scrollViewport = scrollViewport.getParent();
        }
        
        if (scrollViewport == null) {
            // No scroll container found, use the original container
            Rectangle containerBounds = scrollContainer.getBounds();
            return containerBounds.contains(widgetBounds);
        }
        
        // Get the visible viewport bounds (accounting for scroll position)
        Rectangle viewportBounds = scrollViewport.getBounds();
        
        // Adjust the viewport to account for scroll position
        Rectangle visibleArea = new Rectangle(
            viewportBounds.x,
            viewportBounds.y,
            viewportBounds.width,
            viewportBounds.height
        );
        
        // Check if the widget is fully visible within the scrolled viewport
        return visibleArea.contains(widgetBounds);
    }

    private NetDepth getNetDepth(Widget parent, int widgetIndex) {
        Widget depthWidget = parent.getChild(widgetIndex);
        if (depthWidget == null) {
            return null;
        }

        int spriteId = depthWidget.getSpriteId();
        
        if (spriteId == SPRITE_SHALLOW) {
            return NetDepth.SHALLOW;
        } else if (spriteId == SPRITE_MODERATE) {
            return NetDepth.MODERATE;
        } else if (spriteId == SPRITE_DEEP) {
            return NetDepth.DEEP;
        }

        return null;
    }

    private Widget getNetWidget(Widget parent, int index) {
        Widget parentWidget = parent.getChild(index);
        if (parentWidget == null) {
            return null;
        }

        Rectangle bounds = parentWidget.getBounds();

        // Parent widgets have invalid bounds, get their children
        if (bounds.x == -1 && bounds.y == -1) {
            Widget[] children = parentWidget.getChildren();
            if (children != null && children.length > 0) {
                for (Widget child : children) {
                    if (child != null) {
                        Rectangle childBounds = child.getBounds();
                        if (childBounds.x != -1 && childBounds.y != -1) {
                            return child;
                        }
                    }
                }
            }
        } else {
            return parentWidget;
        }

        return null;
    }

    // Enum for net depths
    private enum NetDepth {
        SHALLOW,
        MODERATE,
        DEEP
    }

    /**
     * Data class for exposing timer information to overlay
     */
    public static class TimerInfo {
        private final boolean active;
        private final boolean waiting;
        private final int ticksUntilDepthChange;

        public TimerInfo(boolean active, boolean waiting, int ticksUntilDepthChange) {
            this.active = active;
            this.waiting = waiting;
            this.ticksUntilDepthChange = ticksUntilDepthChange;
        }

        public boolean isActive() {
            return active;
        }

        public boolean isWaiting() {
            return waiting;
        }

        public int getTicksUntilDepthChange() {
            return ticksUntilDepthChange;
        }
    }

    // Data class for shoal timing information
    private static class ShoalTiming {
        final int totalDuration; // Total ticks at each waypoint
        final NetDepth startDepth;
        final NetDepth endDepth;

        ShoalTiming(int totalDuration, NetDepth startDepth, NetDepth endDepth) {
            this.totalDuration = totalDuration;
            this.startDepth = startDepth;
            this.endDepth = endDepth;
        }

        int getDepthChangeTime() {
            return totalDuration / 2;
        }
    }

    // Tracker for shoal timer state - now based on location instead of shoal type
    private class ShoalTracker {
        final int stopDuration;
        final WorldPoint location;
        int ticksAtWaypoint;
        boolean timerActive;

        ShoalTracker(int stopDuration, WorldPoint location) {
            this.stopDuration = stopDuration;
            this.location = location;
            this.ticksAtWaypoint = 0;
            this.timerActive = false; // Don't start timer until we've seen a complete cycle
        }

        void restart() {
            this.ticksAtWaypoint = 0;
            this.timerActive = true; // Activate timer when restarting (after stop→move→stop)
            log.info("Shoal at {} timer restarted and activated (duration: {} ticks)", 
                     location, stopDuration);
        }

        void tick() {
            if (!timerActive) {
                return; // Don't tick until timer is active
            }
            
            ticksAtWaypoint++;
            if (ticksAtWaypoint == 1) {
                log.debug("Shoal at {} timer TICK 1 - timer is now running", location);
            }
            if (ticksAtWaypoint % 10 == 0) {
                NetDepth requiredDepth = getCurrentRequiredDepth();
                log.debug("Shoal at {} at tick {}: required depth = {}", 
                         location, ticksAtWaypoint, requiredDepth);
            }
            
            // Check if we've reached the depth change point - deactivate timer after first depth change
            ShoalTiming timing = DURATION_TO_TIMING.get(stopDuration);
            if (timing != null) {
                int depthChangeTime = timing.getDepthChangeTime();
                
                if (ticksAtWaypoint >= depthChangeTime) {
                    // Depth change has occurred, deactivate timer until shoal moves and stops again
                    timerActive = false;
                    log.debug("Shoal at {} depth change occurred at tick {}, timer deactivated", 
                             location, ticksAtWaypoint);
                }
            }
        }

        NetDepth getCurrentRequiredDepth() {
            if (!timerActive) {
                return null; // Don't provide depth until timer is active
            }
            
            ShoalTiming timing = DURATION_TO_TIMING.get(stopDuration);
            if (timing == null) {
                return null;
            }

            int depthChangeTime = timing.getDepthChangeTime();
            
            if (ticksAtWaypoint < depthChangeTime) {
                return timing.startDepth;
            } else {
                return timing.endDepth;
            }
        }

        TimerInfo getTimerInfo() {
            ShoalTiming timing = DURATION_TO_TIMING.get(stopDuration);
            if (timing == null) {
                return new TimerInfo(false, false, 0);
            }

            // Check if shoal is currently moving (not stopped)
            boolean shoalIsMoving = ticksAtSamePosition < STOPPED_THRESHOLD_TICKS;
            
            if (!timerActive) {
                // Waiting for shoal to stop (either first time or after moving again)
                return new TimerInfo(false, shoalIsMoving, 0);
            }

            int depthChangeTime = timing.getDepthChangeTime();
            
            // Only show timer until first depth change
            int ticksUntilChange = depthChangeTime - ticksAtWaypoint;

            return new TimerInfo(true, false, ticksUntilChange);
        }
    }
}
