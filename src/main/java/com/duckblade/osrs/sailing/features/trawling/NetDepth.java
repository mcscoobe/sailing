package com.duckblade.osrs.sailing.features.trawling;

/**
 * Represents the depth level at which a fishing net can be deployed.
 * Used for tracking both player net depth and shoal depth.
 */
public enum NetDepth
{
	SHALLOW(0),
	MODERATE(1),
	DEEP(2);

	private final int level;

	NetDepth(int level)
	{
		this.level = level;
	}

	public int getLevel()
	{
		return level;
	}

	/**
	 * Checks if this depth is shallower than another depth.
	 *
	 * @param other the depth to compare against
	 * @return true if this depth is shallower than the other depth
	 */
	public boolean isShallowerThan(NetDepth other)
	{
		return this.level < other.level;
	}

	/**
	 * Checks if this depth is deeper than another depth.
	 *
	 * @param other the depth to compare against
	 * @return true if this depth is deeper than the other depth
	 */
	public boolean isDeeperThan(NetDepth other)
	{
		return this.level > other.level;
	}
}
