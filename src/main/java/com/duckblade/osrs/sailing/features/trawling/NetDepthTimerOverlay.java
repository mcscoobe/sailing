package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;

@Slf4j
@Singleton
public class NetDepthTimerOverlay extends OverlayPanel
        implements PluginLifecycleComponent {

    private final NetDepthTimer netDepthTimer;

    @Inject
    public NetDepthTimerOverlay(NetDepthTimer netDepthTimer) {
        this.netDepthTimer = netDepthTimer;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public boolean isEnabled(SailingConfig config) {
        return config.trawlingShowNetDepthTimer();
    }

    @Override
    public void startUp() {
        log.debug("NetDepthTimerOverlay started");
    }

    @Override
    public void shutDown() {
        log.debug("NetDepthTimerOverlay shut down");
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        NetDepthTimer.TimerInfo timerInfo = netDepthTimer.getTimerInfo();
        
        if (timerInfo == null) {
            return null;
        }

        panelComponent.getChildren().clear();

        // Title
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Net Depth Timer")
                .color(Color.CYAN)
                .build());

        if (!timerInfo.isActive()) {
            // Show waiting or calibrating message
            String message = timerInfo.isWaiting() ? "Waiting for shoal to stop" : "Calibrating...";
            Color color = timerInfo.isWaiting() ? Color.ORANGE : Color.YELLOW;
            
            panelComponent.getChildren().add(LineComponent.builder()
                    .left(message)
                    .leftColor(color)
                    .build());
            return super.render(graphics);
        }

        // Show ticks until depth change
        int ticksUntilChange = timerInfo.getTicksUntilDepthChange();
        Color tickColor = ticksUntilChange <= 5 ? Color.RED : Color.WHITE;
        
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Ticks until change:")
                .right(String.valueOf(ticksUntilChange))
                .rightColor(tickColor)
                .build());

        return super.render(graphics);
    }
}
