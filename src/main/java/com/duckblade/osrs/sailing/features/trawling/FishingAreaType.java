package com.duckblade.osrs.sailing.features.trawling;

/**
 * Represents the type of fishing area based on the number of depth levels
 * that shoals can transition through.
 */
public enum FishingAreaType
{
	/**
	 * Standard fishing areas where shoals transition between two depth levels
	 * (e.g., shallow to moderate, or moderate to deep).
	 * Examples: Yellowfin, Halibut areas.
	 */
	TWO_DEPTH,

	/**
	 * Special fishing areas where shoals can transition through all three depth levels
	 * (shallow, moderate, and deep).
	 * Examples: Bluefin, Marlin areas.
	 */
	THREE_DEPTH
}
