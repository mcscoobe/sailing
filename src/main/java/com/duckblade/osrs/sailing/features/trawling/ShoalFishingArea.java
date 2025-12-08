package com.duckblade.osrs.sailing.features.trawling;

import net.runelite.api.coords.WorldPoint;

public class ShoalFishingArea {
	public final int west;
	public final int east;
	public final int south;
	public final int north;

	public ShoalFishingArea(int west, int east, int south, int north) {
		this.west = west;
		this.east = east;
		this.south = south;
		this.north = north;
	}

	public boolean contains(WorldPoint point) {
		int x = point.getX();
		int y = point.getY();
		return x >= west && x <= east && y >= south && y <= north;
	}
}