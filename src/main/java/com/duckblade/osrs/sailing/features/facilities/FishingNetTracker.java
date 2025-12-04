package com.duckblade.osrs.sailing.features.facilities;

import com.duckblade.osrs.sailing.features.util.BoatTracker;
import com.duckblade.osrs.sailing.model.Boat;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.*;
import java.util.ArrayList;

@Slf4j
public class FishingNetTracker extends Overlay
        implements PluginLifecycleComponent {

    private final Client client;
    private final ConfigManager configManager;
    private final BoatTracker boatTracker;

    private static final String CHAT_NET_TOO_DEEP = "Your net is too deep";
    private static final String CHAT_NET_TOO_SHALLOW = "Your net is not deep enough";

    @Inject
    public FishingNetTracker(Client client, ConfigManager configManager, BoatTracker boatTracker)
    {
        this.client = client;
        this.configManager = configManager;
        this.boatTracker = boatTracker;

        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(1000.0f);

    }

    @Subscribe
    public void onChatMessage(ChatMessage e)
    {
        String message = e.getMessage();
        if (message.contains(CHAT_NET_TOO_DEEP)) {
            log.debug("Net too deep");
            render(null);
//            int currentNets = boatTracker.getFishingNets();
//            boatTracker.setFishingNets(currentNets + 1);
        }
        else if (message.contains(CHAT_NET_TOO_SHALLOW)) {
            render(null);
            log.debug("Net too shallow");
//            int currentNets = boatTracker.getFishingNets();
//            boatTracker.setFishingNets(Math.max(0, currentNets - 1));
        }
    }

    private int ExtractFishCaughtCount(String message) {
        // Example message: "You catch 3 fish."
        String[] parts = message.split(" ");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("catch") && i + 1 < parts.length) {
                try {
                    return Integer.parseInt(parts[i + 1]);
                } catch (NumberFormatException ex) {
                    log.debug("Failed to parse fish caught count from message: {}", message);
                }
            }
        }
        return 0;
    }

    // Java
    @Override
    public Dimension render(Graphics2D graphics) {
        int netDownSpriteId = SpriteID.IconChevron16x16._2;
        int netUpSpriteId = SpriteID.IconChevron16x16._4;
        int firstNetDown, firstNetUp, secondNetDown, secondNetUp;
        int rely;
        // chevron down icon id: 897 or 6860?
        // chevron up icon id: 897 or 6862?
        Widget widgetSailingRows = client.getWidget(InterfaceID.SailingSidepanel.FACILITIES_ROWS);
        if (widgetSailingRows == null) {
            return null;
        }
        ArrayList<Widget> nets = new ArrayList<Widget>();
        Widget[] children = widgetSailingRows.getChildren();
        if (children == null) {
            return null;
        }

        Widget firstNet = null;
        int netUp;
        for (Widget child : children) {
            if (child != null && child.getSpriteId() == 897) {
                rely = child.getRelativeY();
                if (rely == 174 || rely == 208)
                {
                    nets.add(child);
                }

                log.debug("Found net down widget at index {} with the id {}", child.getIndex(), child.getId());
            }
            else if (child != null && child.getSpriteId() == 897) {
                rely = child.getRelativeY();
                if (rely == 174 || rely == 208)
                {
                    nets.add(child);
                }
                log.debug("Found net up widget at index {} with the id {}", child.getIndex(), child.getId());
            }
        }


        // Highlight around the widget
        for (Widget netWidget : nets) {
            if (netWidget == null) {
                continue;
            }
            Rectangle bounds = netWidget.getBounds();
            graphics.setColor(Color.YELLOW);
            graphics.setStroke(new BasicStroke(2));
            graphics.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        return null;
    }

}
