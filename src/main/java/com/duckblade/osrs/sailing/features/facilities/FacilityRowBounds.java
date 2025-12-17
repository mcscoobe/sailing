package com.duckblade.osrs.sailing.features.facilities;

import java.awt.Rectangle;

/**
 * Contains positioning and state information for a facility row in the drag interface overlay.
 */
public class FacilityRowBounds
{
	private final Rectangle bounds;
	private final FacilityRow row;
	private final boolean isDropTarget;
	private final boolean isDragged;
	
	public FacilityRowBounds(Rectangle bounds, FacilityRow row, boolean isDropTarget, boolean isDragged)
	{
		this.bounds = bounds;
		this.row = row;
		this.isDropTarget = isDropTarget;
		this.isDragged = isDragged;
	}
	
	/**
	 * Gets the bounding rectangle for this facility row.
	 */
	public Rectangle getBounds()
	{
		return bounds;
	}
	
	/**
	 * Gets the facility row this bounds object represents.
	 */
	public FacilityRow getRow()
	{
		return row;
	}
	
	/**
	 * Returns true if this row is a valid drop target during a drag operation.
	 */
	public boolean isDropTarget()
	{
		return isDropTarget;
	}
	
	/**
	 * Returns true if this row is currently being dragged.
	 */
	public boolean isDragged()
	{
		return isDragged;
	}
}