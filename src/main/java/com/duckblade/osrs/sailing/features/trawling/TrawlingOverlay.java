package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;

/**
 * Combined overlay for trawling features including net capacity and depth timer
 */
@Slf4j
@Singleton
public class TrawlingOverlay extends OverlayPanel
        implements PluginLifecycleComponent {

    private final Client client;
    private final NetCapacityTracker netCapacityTracker;
    private final NetDepthTimer netDepthTimer;
    private final SailingConfig config;

    @Inject
    public TrawlingOverlay(Client client, NetCapacityTracker netCapacityTracker, NetDepthTimer netDepthTimer, SailingConfig config) {
        this.client = client;
        this.netCapacityTracker = netCapacityTracker;
        this.netDepthTimer = netDepthTimer;
        this.config = config;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public boolean isEnabled(SailingConfig config) {
        // Enable if either feature is enabled
        return config.trawlingShowNetCapacity() || config.trawlingShowNetDepthTimer();
    }

    @Override
    public void startUp() {
        log.debug("TrawlingOverlay started");
    }

    @Override
    public void shutDown() {
        log.debug("TrawlingOverlay shut down");
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!SailingUtil.isSailing(client)) {
            return null;
        }

        panelComponent.getChildren().clear();
        boolean hasContent = false;

        // Add net depth timer section if enabled and available
        if (shouldShowDepthTimer()) {
            NetDepthTimer.TimerInfo timerInfo = netDepthTimer.getTimerInfo();
            if (timerInfo != null) {
                if (!hasContent) {
                    panelComponent.getChildren().add(TitleComponent.builder()
                            .text("Trawling")
                            .color(Color.CYAN)
                            .build());
                    hasContent = true;
                }

                if (!timerInfo.isActive()) {
                    // Show waiting or calibrating message
                    String message = timerInfo.isWaiting() ? "Waiting for shoal to stop" : "Calibrating...";
                    Color color = timerInfo.isWaiting() ? Color.ORANGE : Color.YELLOW;
                    
                    panelComponent.getChildren().add(LineComponent.builder()
                            .left("Depth Timer:")
                            .right(message)
                            .rightColor(color)
                            .build());
                } else {
                    // Show ticks until depth change
                    int ticksUntilChange = timerInfo.getTicksUntilDepthChange();
                    Color tickColor = ticksUntilChange <= 5 ? Color.RED : Color.WHITE;
                    
                    panelComponent.getChildren().add(LineComponent.builder()
                            .left("Depth Change:")
                            .right(ticksUntilChange + " ticks")
                            .rightColor(tickColor)
                            .build());
                }
            }
        }

        // Add net capacity section if enabled and available
        if (shouldShowNetCapacity()) {
            int maxCapacity = netCapacityTracker.getMaxCapacity();
            if (maxCapacity > 0) {
                if (!hasContent) {
                    panelComponent.getChildren().add(TitleComponent.builder()
                            .text("Trawling")
                            .color(Color.CYAN)
                            .build());
                    hasContent = true;
                }

                int totalFishCount = netCapacityTracker.getTotalFishCount();

                // Choose color based on how full the nets are
                Color textColor;
                float fillPercent = (float) totalFishCount / maxCapacity;
                if (fillPercent >= 0.9f) {
                    textColor = Color.RED; // Nearly full
                } else if (fillPercent >= 0.7f) {
                    textColor = Color.ORANGE; // Getting full
                } else {
                    textColor = Color.WHITE; // Plenty of space
                }

                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Net Capacity:")
                        .right(totalFishCount + "/" + maxCapacity)
                        .rightColor(textColor)
                        .build());
            }
        }

        return hasContent ? super.render(graphics) : null;
    }

    private boolean shouldShowDepthTimer() {
        return config.trawlingShowNetDepthTimer();
    }

    private boolean shouldShowNetCapacity() {
        return config.trawlingShowNetCapacity();
    }
}