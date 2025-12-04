package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.google.common.collect.ImmutableSet;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.ui.overlay.Overlay;

import java.awt.*;
import java.util.Set;

public class ShoalOverlay extends Overlay
        implements PluginLifecycleComponent {

    private static final Set<Integer> RAPIDS_IDS = ImmutableSet.of(
            ObjectID.SAILING_SHOAL_CLICKBOX_BLUEFIN,
            ObjectID.SAILING_SHOAL_CLICKBOX_GIANT_KRILL,
            ObjectID.SAILING_SHOAL_CLICKBOX_GLISTENING,
            ObjectID.SAILING_SHOAL_CLICKBOX_HADDOCK,
            ObjectID.SAILING_SHOAL_CLICKBOX_HALIBUT,
            ObjectID.SAILING_SHOAL_CLICKBOX_MARLIN,
            ObjectID.SAILING_SHOAL_CLICKBOX_SHIMMERING,
            ObjectID.SAILING_SHOAL_CLICKBOX_VIBRANT,
            ObjectID.SAILING_SHOAL_CLICKBOX_YELLOWFIN);

    @Override
    public Dimension render(Graphics2D graphics) {
        return null;
    }
}
