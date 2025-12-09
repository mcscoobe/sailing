package com.duckblade.osrs.sailing.features.trawling;

import net.runelite.api.gameval.ObjectID;

public class TrawlingData {

    public static class ShoalObjectID {
        protected static final int GIANT_KRILL = ObjectID.SAILING_SHOAL_CLICKBOX_GIANT_KRILL;
        protected static final int HADDOCK = ObjectID.SAILING_SHOAL_CLICKBOX_HADDOCK;
        protected static final int YELLOWFIN = ObjectID.SAILING_SHOAL_CLICKBOX_YELLOWFIN;
        protected static final int HALIBUT = ObjectID.SAILING_SHOAL_CLICKBOX_HALIBUT;
        protected static final int BLUEFIN = ObjectID.SAILING_SHOAL_CLICKBOX_BLUEFIN;
        protected static final int MARLIN = ObjectID.SAILING_SHOAL_CLICKBOX_MARLIN;
        protected static final int SHIMMERING = ObjectID.SAILING_SHOAL_CLICKBOX_SHIMMERING;
        protected static final int GLISTENING = ObjectID.SAILING_SHOAL_CLICKBOX_GLISTENING;
        protected static final int VIBRANT = ObjectID.SAILING_SHOAL_CLICKBOX_VIBRANT;
    }

    public static class ShoalStopDuration {

        protected static final int YELLOWFIN = 100;
        protected static final int HALIBUT = 80;
        protected static final int BLUEFIN = 66;
        protected static final int MARLIN = 50;
    }

    public static class FishingAreas {
        protected static final ShoalFishingArea PORT_ROBERTS = new ShoalFishingArea(1822, 2050, 3129, 3414);
        protected static final ShoalFishingArea SOUTHERN_EXPANSE = new ShoalFishingArea(1870, 2180, 2171, 2512);
        protected static final ShoalFishingArea RAINBOW_REEF = new ShoalFishingArea(2075, 2406, 2179, 2450);
        protected static final ShoalFishingArea BUCCANEERS_HAVEN = new ShoalFishingArea(1984, 2268, 3594, 3771);
        protected static final ShoalFishingArea WEISSMERE = new ShoalFishingArea(2590, 2870, 3945, 4146);
    }
}
