package com.duckblade.osrs.sailing.features.trawling;

/**
 * Represents the direction of shoal movement between depth levels.
 * Used in three-depth fishing areas to track whether the shoal is moving
 * from moderate depth to shallow or deep.
 */
public enum MovementDirection
{
	/**
	 * Shoal is moving from moderate depth to shallow depth.
	 */
	SHALLOWER,

	/**
	 * Shoal is moving from moderate depth to deep depth.
	 */
	DEEPER,

	/**
	 * Movement direction has not been detected yet.
	 */
	UNKNOWN
}
