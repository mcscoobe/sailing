package com.duckblade.osrs.sailing.features.facilities;

/**
 * Represents the current state of the drag interface for facility row reordering.
 */
public enum DragState
{
	/**
	 * Reorder mode is disabled, no visual indicators should be shown.
	 */
	DISABLED,
	
	/**
	 * Reorder mode is enabled, draggable outlines should be visible.
	 */
	ENABLED,
	
	/**
	 * An active drag operation is in progress.
	 */
	DRAGGING,
	
	/**
	 * Brief state during drop operation completion.
	 */
	DROPPING
}