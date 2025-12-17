package com.duckblade.osrs.sailing.features.facilities;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.DraggingWidgetChanged;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.runelite.api.widgets.WidgetConfig.DRAG;
import static net.runelite.api.widgets.WidgetConfig.DRAG_ON;

/**
 * Drag and drop reordering system for sailing facility rows, following RuneLite's standard pattern.
 * Uses DraggingWidgetChanged events and widget drag flags like prayer and spellbook plugins.
 */
@Slf4j
@Singleton
public class SailingInterfaceRepositioner implements PluginLifecycleComponent
{
	private static final int SAILING_SIDEPANEL_GROUP_ID = 937;
	private static final int FACILITIES_ROWS_WIDGET_ID = 0x03a9_001b;
	
	// Row widget ranges (each row contains multiple widgets)
	private static final int HELM_ROW_START = 0;
	private static final int HELM_ROW_END = 45;
	private static final int REPAIRS_ROW_START = 46;
	private static final int REPAIRS_ROW_END = 59;
	private static final int BOOSTS_ROW_START = 60;
	private static final int BOOSTS_ROW_END = 82;
	private static final int CHUM_ROW_START = 83;
	private static final int CHUM_ROW_END = 95;
	private static final int NET_ONE_ROW_START = 96;
	private static final int NET_ONE_ROW_END = 129;
	private static final int NET_TWO_ROW_START = 130;
	private static final int NET_TWO_ROW_END = 164;
	
	private static final String CONFIG_GROUP = "sailing";
	private static final String CONFIG_KEY = "facilityRowOrder";
	
	private final Client client;
	private final SailingConfig config;
	private final ConfigManager configManager;
	
	@Inject
	public SailingInterfaceRepositioner(Client client, SailingConfig config, ConfigManager configManager)
	{
		this.client = client;
		this.config = config;
		this.configManager = configManager;
	}
	
	private boolean reorderMode = false;
	private List<FacilityRow> facilityRows;
	private boolean needsReordering = false;
	private DragState currentDragState = DragState.DISABLED;
	
	@Override
	public boolean isEnabled(SailingConfig config)
	{
		return true; // Always enabled - config controls unlock/lock state
	}
	

	
	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		// Check if the facilities rows widget is loaded
		if (event.getGroupId() == (FACILITIES_ROWS_WIDGET_ID >> 16))
		{
			log.debug("Facilities interface loaded");
			needsReordering = true;
		}
	}
	
	@Subscribe
	public void onDraggingWidgetChanged(DraggingWidgetChanged event)
	{
		// Log all drag events to understand what's happening
		log.info("DraggingWidgetChanged: isDragging={}, mouseButton={}", 
			event.isDraggingWidget(), client.getMouseCurrentButton());
		
		Widget draggedWidget = client.getDraggedWidget();
		Widget draggedOnWidget = client.getDraggedOnWidget();
		
		// ENHANCED: Log widget IDs and calculate child indices
		int draggedChildIndex = -1;
		int draggedOnChildIndex = -1;
		
		if (draggedWidget != null)
		{
			draggedChildIndex = calculateChildIndex(draggedWidget.getId());
		}
		
		if (draggedOnWidget != null)
		{
			draggedOnChildIndex = calculateChildIndex(draggedOnWidget.getId());
		}
		
		log.info("Drag widgets: dragged={} (child {}), draggedOn={} (child {})", 
			draggedWidget != null ? draggedWidget.getId() : "null", draggedChildIndex,
			draggedOnWidget != null ? draggedOnWidget.getId() : "null", draggedOnChildIndex);
		
		// Handle drag and drop when mouse button is released during drag
		// This matches the prayer plugin pattern exactly
		if (event.isDraggingWidget() && client.getMouseCurrentButton() == 0)
		{
			if (draggedWidget == null)
			{
				log.info("Drag cancelled: no dragged widget");
				return;
			}
			
			if (draggedOnWidget == null)
			{
				log.info("Drag cancelled: no drop target widget (try dragging to a different facility row)");
				return;
			}
			
			// Check if both widgets belong to the facilities interface
			int draggedGroupId = WidgetUtil.componentToInterface(draggedWidget.getId());
			int draggedOnGroupId = WidgetUtil.componentToInterface(draggedOnWidget.getId());
			int facilitiesGroupId = FACILITIES_ROWS_WIDGET_ID >> 16;
			
			log.info("Widget groups: dragged=0x{}, draggedOn=0x{}, facilities=0x{}", 
				Integer.toHexString(draggedGroupId), 
				Integer.toHexString(draggedOnGroupId), 
				Integer.toHexString(facilitiesGroupId));
			
			if (draggedGroupId != facilitiesGroupId || draggedOnGroupId != facilitiesGroupId)
			{
				log.info("Drag not for facilities interface");
				return;
			}
			
			// Find which facility rows these widgets belong to
			FacilityRow fromRow = getRowForWidget(draggedWidget.getId());
			FacilityRow toRow = getRowForWidget(draggedOnWidget.getId());
			
			log.info("Facility rows: from={}, to={}", 
				fromRow != null ? fromRow.getName() : "null",
				toRow != null ? toRow.getName() : "null");
			
			if (fromRow == null || toRow == null || fromRow == toRow)
			{
				log.info("Drag cancelled: invalid rows or same row");
				return;
			}
			
			// Check if both rows are draggable (not Helm or Repairs)
			boolean fromDraggable = !("Helm".equals(fromRow.getName()) || "Repairs".equals(fromRow.getName()));
			boolean toDraggable = !("Helm".equals(toRow.getName()) || "Repairs".equals(toRow.getName()));
			
			if (!fromDraggable || !toDraggable)
			{
				log.info("Drag cancelled: {} row (draggable: {}) to {} row (draggable: {}) - only draggable facility rows can be reordered", 
					fromRow.getName(), fromDraggable, toRow.getName(), toDraggable);
				return;
			}
			
			log.info("Dragging {} row to {} row position", fromRow.getName(), toRow.getName());
			
			// Reset dragged on widget to prevent sending drag packet to server
			client.setDraggedOnWidget(null);
			
			// Perform the reorder
			reorderFacilityRows(fromRow, toRow);
		}
	}
	
	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		// Update reorder mode when config changes
		updateReorderMode();
		
		// CRITICAL FIX: Only apply reordering once per interface load, not on every script
		if (needsReordering && event.getScriptId() == 6388)
		{
			log.info("Sailing interface script {} fired, applying row order (ONCE)", event.getScriptId());
			applyRowOrder();
			needsReordering = false;
		}
		
		// CRITICAL FIX: Only rebuild drag configuration once, not on every script
		if (event.getScriptId() == 6388 && reorderMode)
		{
			log.info("Rebuilding facility rows due to script {} (ONCE)", event.getScriptId());
			rebuildFacilityRows(reorderMode);
		}
		
		// Enhanced script monitoring for debugging - log all scripts when interface is open
		if (debugMode && isSailingInterfaceOpen())
		{
			log.info("DEBUG: Script {} fired while sailing interface open", event.getScriptId());
			
			// Check if this script might be resetting widget configurations
			if (!isSailingInterfaceScript(event.getScriptId()))
			{
				// This might be a script that's interfering with our drag configuration
				log.warn("DEBUG: Unknown script {} fired - may interfere with drag config", event.getScriptId());
			}
		}
	}
	
	private boolean isSailingInterfaceScript(int scriptId)
	{
		// These are common script IDs that might handle sailing interface layout
		// We may need to adjust these based on testing with RuneLite developer tools
		// Use Script Inspector to identify which scripts fire when sailing interface changes
		return scriptId == 6385 || scriptId == 6386 || scriptId == 937 || 
		       scriptId == 1001 || scriptId == 1002 || scriptId == 6387 || scriptId == 6388; // Add more as needed
	}
	
	/**
	 * Updates reorder mode based on current configuration.
	 * Public for testing purposes.
	 */
	public void updateReorderMode()
	{
		boolean configReorderMode = config.reorderFacilityRows();
		if (reorderMode != configReorderMode)
		{
			reorderMode = configReorderMode;
			
			if (reorderMode)
			{
				setDragState(DragState.ENABLED);
				log.info("Facility reorder mode ENABLED - drag facility rows to reorder them");
			}
			else
			{
				setDragState(DragState.DISABLED);
				log.info("Facility reorder mode DISABLED - row order locked in place");
				saveRowOrder();
			}
			
			// Rebuild widgets with new drag configuration immediately
			log.info("Rebuilding facility rows due to reorder mode change: {}", reorderMode);
			rebuildFacilityRows(reorderMode);
		}
	}
	
	private void rebuildFacilityRows(boolean unlocked)
	{
		Widget facilitiesWidget = client.getWidget(FACILITIES_ROWS_WIDGET_ID);
		if (facilitiesWidget == null)
		{
			log.warn("Facilities widget not found for rebuild (ID: 0x{})", Integer.toHexString(FACILITIES_ROWS_WIDGET_ID));
			return;
		}
		
		Widget[] children = facilitiesWidget.getChildren();
		if (children == null)
		{
			log.warn("No children found in facilities widget for rebuild");
			return;
		}
		
		log.info("Rebuilding {} facility rows with drag enabled: {} (widget has {} children)", 
			facilityRows.size(), unlocked, children.length);
		
		// Configure drag flags for facility row widgets following prayer plugin pattern
		// Only configure draggable rows (exclude Helm and Repairs)
		for (FacilityRow row : facilityRows)
		{
			boolean isDraggable = !("Helm".equals(row.getName()) || "Repairs".equals(row.getName()));
			int widgetsConfigured = 0;
			
			log.info("Configuring {} row (draggable: {})", row.getName(), isDraggable);
			
			// Configure ALL widgets in each row - this is critical for proper drop target detection
			for (int widgetIndex = row.getStartIndex(); widgetIndex <= row.getEndIndex() && widgetIndex < children.length; widgetIndex++)
			{
				Widget widget = children[widgetIndex];
				
				if (widget != null)
				{
					int originalConfig = widget.getClickMask();
					int newConfig;
					
					if (unlocked && isDraggable)
					{
						// Enable dragging of this widget and allow it to be dragged on
						// This matches the prayer plugin pattern exactly
						newConfig = originalConfig | DRAG | DRAG_ON;
					}
					else
					{
						// Remove drag flags (either disabled or non-draggable row)
						newConfig = originalConfig & ~(DRAG | DRAG_ON);
					}
					
					widget.setClickMask(newConfig);
					widgetsConfigured++;
					
					// Log all widget configurations for debugging
					log.info("Widget {} (ID: 0x{}): config 0x{} -> 0x{} (DRAG={}, DRAG_ON={}, hidden={})", 
						widgetIndex, Integer.toHexString(widget.getId()), 
						Integer.toHexString(originalConfig), 
						Integer.toHexString(newConfig),
						(newConfig & DRAG) != 0,
						(newConfig & DRAG_ON) != 0,
						widget.isHidden());
				}
			}
			
			log.info("Configured {} widgets for {} row (indices {}-{}, draggable: {})", 
				widgetsConfigured, row.getName(), row.getStartIndex(), row.getEndIndex(), isDraggable);
		}
		
		log.info("Completed rebuilding facility rows with drag enabled: {}", unlocked);
	}
	
	private void reorderFacilityRows(FacilityRow fromRow, FacilityRow toRow)
	{
		// Only work with draggable rows for reordering
		List<FacilityRow> draggableRows = facilityRows.stream()
			.filter(row -> !("Helm".equals(row.getName()) || "Repairs".equals(row.getName())))
			.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
		
		int fromIndex = draggableRows.indexOf(fromRow);
		int toIndex = draggableRows.indexOf(toRow);
		
		if (fromIndex == -1 || toIndex == -1)
		{
			log.warn("Could not find draggable row indices: from={}, to={}", fromIndex, toIndex);
			return;
		}
		
		log.info("Moving {} row from position {} to position {} (among draggable rows)", fromRow.getName(), fromIndex, toIndex);
		
		// Remove from current position
		draggableRows.remove(fromIndex);
		
		// Insert at new position
		if (fromIndex < toIndex)
		{
			draggableRows.add(toIndex - 1, fromRow);
		}
		else
		{
			draggableRows.add(toIndex, fromRow);
		}
		
		// Rebuild the full facility rows list with new draggable order
		facilityRows.clear();
		facilityRows.add(new FacilityRow("Helm", HELM_ROW_START, HELM_ROW_END));
		facilityRows.add(new FacilityRow("Repairs", REPAIRS_ROW_START, REPAIRS_ROW_END));
		facilityRows.addAll(draggableRows);
		
		log.info("New draggable row order: {}", draggableRows.stream().map(FacilityRow::getName).toArray());
		
		// Apply the new order and save it
		needsReordering = true;
		applyRowOrder();
		saveRowOrder();
	}
	

	
	private void initializeFacilityRows()
	{
		facilityRows = new ArrayList<>();
		// Include ALL facility rows for proper widget detection
		// But only some will be draggable (marked in rebuildFacilityRows)
		facilityRows.add(new FacilityRow("Helm", HELM_ROW_START, HELM_ROW_END));
		facilityRows.add(new FacilityRow("Repairs", REPAIRS_ROW_START, REPAIRS_ROW_END));
		facilityRows.add(new FacilityRow("Boosts", BOOSTS_ROW_START, BOOSTS_ROW_END));
		facilityRows.add(new FacilityRow("Chum", CHUM_ROW_START, CHUM_ROW_END));
		facilityRows.add(new FacilityRow("Net One", NET_ONE_ROW_START, NET_ONE_ROW_END));
		facilityRows.add(new FacilityRow("Net Two", NET_TWO_ROW_START, NET_TWO_ROW_END));
	}
	
	/**
	 * Find which facility row a widget belongs to
	 * Returns the row even if it's non-draggable, for proper detection
	 */
	private FacilityRow getRowForWidget(int widgetId)
	{
		Widget widget = client.getWidget(widgetId);
		if (widget == null)
		{
			return null;
		}
		
		Widget facilitiesWidget = client.getWidget(FACILITIES_ROWS_WIDGET_ID);
		if (facilitiesWidget == null)
		{
			return null;
		}
		
		Widget[] children = facilitiesWidget.getChildren();
		if (children == null)
		{
			return null;
		}
		
		// Find the widget index in the children array
		int widgetIndex = -1;
		for (int i = 0; i < children.length; i++)
		{
			if (children[i] != null && children[i].getId() == widgetId)
			{
				widgetIndex = i;
				break;
			}
		}
		
		if (widgetIndex == -1)
		{
			log.debug("Widget {} not found in facilities children", widgetId);
			return null;
		}
		
		// Find which facility row this widget index belongs to
		for (FacilityRow row : facilityRows)
		{
			if (widgetIndex >= row.getStartIndex() && widgetIndex <= row.getEndIndex())
			{
				log.debug("Widget {} (child {}) belongs to {} row", widgetId, widgetIndex, row.getName());
				return row;
			}
		}
		
		log.debug("Widget {} (child {}) does not belong to any facility row", widgetId, widgetIndex);
		return null;
	}
	
	private void saveRowOrder()
	{
		// Only save draggable row order
		String[] rowNames = facilityRows.stream()
			.filter(row -> !("Helm".equals(row.getName()) || "Repairs".equals(row.getName())))
			.map(r -> r.getName())
			.toArray(String[]::new);
		String orderString = String.join(",", rowNames);
		configManager.setConfiguration(CONFIG_GROUP, CONFIG_KEY, orderString);
		log.debug("Saved draggable row order: {}", orderString);
	}
	
	public void loadRowOrder()
	{
		String orderString = configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY);
		if (orderString == null || orderString.isEmpty())
		{
			return; // Use default order
		}
		
		String[] rowNames = orderString.split(",");
		List<FacilityRow> draggableRows = new ArrayList<>();
		
		// Get current draggable rows
		List<FacilityRow> currentDraggable = facilityRows.stream()
			.filter(row -> !("Helm".equals(row.getName()) || "Repairs".equals(row.getName())))
			.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
		
		// Rebuild draggable list in saved order
		for (String name : rowNames)
		{
			currentDraggable.stream()
				.filter(row -> row.getName().equals(name))
				.findFirst()
				.ifPresent(draggableRows::add);
		}
		
		// Add any missing draggable rows (in case of config corruption)
		for (FacilityRow row : currentDraggable)
		{
			if (!draggableRows.contains(row))
			{
				draggableRows.add(row);
			}
		}
		
		// Rebuild full list with fixed Helm/Repairs at top
		facilityRows.clear();
		facilityRows.add(new FacilityRow("Helm", HELM_ROW_START, HELM_ROW_END));
		facilityRows.add(new FacilityRow("Repairs", REPAIRS_ROW_START, REPAIRS_ROW_END));
		facilityRows.addAll(draggableRows);
		
		log.debug("Loaded draggable row order: {}", Arrays.toString(rowNames));
	}
	
	public void applyRowOrder()
	{
		Widget facilitiesWidget = client.getWidget(FACILITIES_ROWS_WIDGET_ID);
		if (facilitiesWidget == null)
		{
			log.debug("Facilities widget not found, cannot apply row order");
			return;
		}
		
		Widget[] allChildren = facilitiesWidget.getChildren();
		if (allChildren == null || allChildren.length == 0)
		{
			log.debug("No children found in facilities widget");
			return;
		}
		
		log.info("Applying facility row order: {}", facilityRows.stream().map(r -> r.getName()).toArray());
		
		// CRITICAL FIX: Don't reposition widgets if we don't have a custom order
		// This prevents breaking the default layout
		String savedOrder = configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY);
		if (savedOrder == null || savedOrder.isEmpty())
		{
			log.info("No custom row order saved, keeping default layout");
			return;
		}
		
		// Store original Y positions for facility rows
		int[] originalRowYPositions = new int[6];
		originalRowYPositions[0] = getRowFirstWidgetY(allChildren, HELM_ROW_START);
		originalRowYPositions[1] = getRowFirstWidgetY(allChildren, REPAIRS_ROW_START);
		originalRowYPositions[2] = getRowFirstWidgetY(allChildren, BOOSTS_ROW_START);
		originalRowYPositions[3] = getRowFirstWidgetY(allChildren, CHUM_ROW_START);
		originalRowYPositions[4] = getRowFirstWidgetY(allChildren, NET_ONE_ROW_START);
		originalRowYPositions[5] = getRowFirstWidgetY(allChildren, NET_TWO_ROW_START);
		
		// CRITICAL FIX: Validate Y positions before applying
		for (int i = 0; i < originalRowYPositions.length; i++)
		{
			if (originalRowYPositions[i] <= 0)
			{
				log.warn("Invalid Y position {} for row index {}, skipping row reordering", originalRowYPositions[i], i);
				return;
			}
		}
		
		// Apply new positions based on current order
		// Only reposition draggable rows (skip Helm and Repairs)
		List<FacilityRow> draggableRows = facilityRows.stream()
			.filter(row -> !("Helm".equals(row.getName()) || "Repairs".equals(row.getName())))
			.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
		
		for (int newPos = 0; newPos < draggableRows.size(); newPos++)
		{
			FacilityRow row = draggableRows.get(newPos);
			int targetY = originalRowYPositions[2 + newPos]; // Start from Boosts position (index 2)
			int originalRowPos = getOriginalRowPosition(row);
			int originalY = originalRowYPositions[originalRowPos];
			
			log.debug("Moving {} row from Y={} to Y={}", row.getName(), originalY, targetY);
			
			// Move all widgets in this row
			for (int widgetIndex = row.getStartIndex(); widgetIndex <= row.getEndIndex() && widgetIndex < allChildren.length; widgetIndex++)
			{
				Widget widget = allChildren[widgetIndex];
				if (widget != null)
				{
					int currentY = widget.getOriginalY();
					int offsetFromOriginal = currentY - originalY;
					int newY = targetY + offsetFromOriginal;
					
					// CRITICAL FIX: Ensure new Y position is valid
					if (newY > 0)
					{
						widget.setOriginalY(newY);
						widget.revalidate();
					}
					else
					{
						log.warn("Skipping invalid Y position {} for widget {}", newY, widgetIndex);
					}
				}
			}
		}
		
		log.info("Successfully applied row order");
	}
	
	private int getRowFirstWidgetY(Widget[] allChildren, int rowStartIndex)
	{
		if (rowStartIndex < allChildren.length && allChildren[rowStartIndex] != null)
		{
			return allChildren[rowStartIndex].getOriginalY();
		}
		return 0; // Fallback
	}
	
	private int getOriginalRowPosition(FacilityRow row)
	{
		// Return the original position index for this row
		if (row.getStartIndex() == HELM_ROW_START) return 0;
		if (row.getStartIndex() == REPAIRS_ROW_START) return 1;
		if (row.getStartIndex() == BOOSTS_ROW_START) return 2;
		if (row.getStartIndex() == CHUM_ROW_START) return 3;
		if (row.getStartIndex() == NET_ONE_ROW_START) return 4;
		if (row.getStartIndex() == NET_TWO_ROW_START) return 5;
		return 0;
	}
	
	// Drag state management methods
	
	/**
	 * Sets the current drag state and validates the transition.
	 * Notifies the overlay of state changes for visual updates.
	 */
	private void setDragState(DragState newState)
	{
		if (isValidStateTransition(currentDragState, newState))
		{
			DragState previousState = currentDragState;
			currentDragState = newState;
			log.debug("Drag state transition: {} -> {}", previousState, newState);
			
			// Notify overlay of state change for visual updates
			notifyOverlayStateChange(previousState, newState);
		}
		else
		{
			log.warn("Invalid drag state transition attempted: {} -> {}", currentDragState, newState);
		}
	}
	
	/**
	 * Gets the current drag state.
	 */
	public DragState getCurrentDragState()
	{
		return currentDragState;
	}
	

	
	/**
	 * Gets all facility rows in their current order.
	 */
	public List<FacilityRow> getFacilityRows()
	{
		return new ArrayList<>(facilityRows);
	}
	
	/**
	 * Validates whether a state transition is allowed.
	 */
	private boolean isValidStateTransition(DragState from, DragState to)
	{
		switch (from)
		{
			case DISABLED:
				return to == DragState.ENABLED;
			case ENABLED:
				return to == DragState.DISABLED || to == DragState.DRAGGING;
			case DRAGGING:
				return to == DragState.ENABLED || to == DragState.DROPPING;
			case DROPPING:
				return to == DragState.ENABLED;
			default:
				return false;
		}
	}
	
	/**
	 * Notifies the overlay of drag state changes for visual updates.
	 * This ensures the visual feedback stays synchronized with the logical state.
	 */
	private void notifyOverlayStateChange(DragState previousState, DragState newState)
	{
		// The overlay will automatically pick up the new state on its next render cycle
		// by calling getCurrentDragState() and getDraggedRow()
		log.debug("Notified overlay of drag state change: {} -> {}", previousState, newState);
	}
	
	/**
	 * Checks if reorder mode is currently enabled.
	 */
	public boolean isReorderModeEnabled()
	{
		return reorderMode;
	}
	
	/**
	 * Checks if there is an active drag operation.
	 */
	public boolean isDragInProgress()
	{
		return currentDragState == DragState.DRAGGING || currentDragState == DragState.DROPPING;
	}
	
	/**
	 * Gets the widget ID for the facilities rows container.
	 */
	public static int getFacilitiesRowsWidgetId()
	{
		return FACILITIES_ROWS_WIDGET_ID;
	}
	
	/**
	 * Forces a refresh of the visual state in the overlay.
	 * This can be called when the interface state changes outside of normal drag operations.
	 */
	public void refreshVisualState()
	{
		// The overlay will pick up the current state on its next render cycle
		log.debug("Visual state refresh requested");
	}
	

	
	/**
	 * Resets the facility rows to their default order.
	 * This method is public to allow testing and external control.
	 */
	public void resetToDefaultOrder()
	{
		log.info("Resetting facility rows to default order");
		initializeFacilityRows();
		needsReordering = true;
		applyRowOrder();
		saveRowOrder();
		
		// Rebuild widgets with current drag configuration
		rebuildFacilityRows(reorderMode);
	}
	
	/**
	 * Gets the currently dragged row (always null in new implementation).
	 * Kept for overlay compatibility.
	 */
	public FacilityRow getDraggedRow()
	{
		return null; // No persistent drag state in new implementation
	}
	
	/**
	 * Checks if a given facility row is a valid drop target.
	 * In the new implementation, all rows are valid drop targets when reordering is enabled.
	 */
	public boolean isValidDropTarget(FacilityRow row)
	{
		return reorderMode && row != null;
	}
	
	/**
	 * Check if the sailing interface is currently open and visible
	 */
	public boolean isSailingInterfaceOpen()
	{
		Widget facilitiesWidget = client.getWidget(FACILITIES_ROWS_WIDGET_ID);
		return facilitiesWidget != null && !facilitiesWidget.isHidden();
	}
	
	/**
	 * Manual method to test drag configuration - can be called from debug console
	 */
	public void testDragConfiguration()
	{
		log.info("=== TESTING DRAG CONFIGURATION ===");
		log.info("Reorder mode: {}", reorderMode);
		log.info("Drag state: {}", currentDragState);
		log.info("Sailing interface open: {}", isSailingInterfaceOpen());
		
		Widget facilitiesWidget = client.getWidget(FACILITIES_ROWS_WIDGET_ID);
		if (facilitiesWidget == null)
		{
			log.info("Facilities widget not found! (ID: 0x{})", Integer.toHexString(FACILITIES_ROWS_WIDGET_ID));
			return;
		}
		
		log.info("Facilities widget found: ID=0x{}, hidden={}, bounds=({},{},{},{})", 
			Integer.toHexString(facilitiesWidget.getId()),
			facilitiesWidget.isHidden(),
			facilitiesWidget.getBounds().x, facilitiesWidget.getBounds().y,
			facilitiesWidget.getBounds().width, facilitiesWidget.getBounds().height);
		
		Widget[] children = facilitiesWidget.getChildren();
		if (children == null)
		{
			log.info("No children in facilities widget!");
			return;
		}
		
		log.info("Facilities widget has {} children", children.length);
		
		for (FacilityRow row : facilityRows)
		{
			log.info("Testing {} row (indices {}-{})", row.getName(), row.getStartIndex(), row.getEndIndex());
			
			for (int i = 0; i < 3 && (row.getStartIndex() + i) <= row.getEndIndex() && (row.getStartIndex() + i) < children.length; i++)
			{
				int widgetIndex = row.getStartIndex() + i;
				Widget widget = children[widgetIndex];
				
				if (widget != null)
				{
					int config = widget.getClickMask();
					boolean hasDrag = (config & DRAG) != 0;
					boolean hasDragOn = (config & DRAG_ON) != 0;
					
					log.info("  Widget {} (ID: 0x{}): config=0x{}, DRAG={}, DRAG_ON={}, hidden={}, bounds=({},{},{},{})", 
						widgetIndex, Integer.toHexString(widget.getId()), 
						Integer.toHexString(config), hasDrag, hasDragOn, widget.isHidden(),
						widget.getBounds().x, widget.getBounds().y,
						widget.getBounds().width, widget.getBounds().height);
				}
				else
				{
					log.info("  Widget {} is null", widgetIndex);
				}
			}
		}
		
		log.info("=== END DRAG CONFIGURATION TEST ===");
	}
	
	/**
	 * Forces a rebuild of the drag configuration - useful for testing
	 */
	public void forceDragRebuild()
	{
		log.info("=== FORCING DRAG REBUILD ===");
		rebuildFacilityRows(reorderMode);
		log.info("=== DRAG REBUILD COMPLETE ===");
	}
	
	/**
	 * Enable reorder mode for testing
	 */
	public void enableReorderMode()
	{
		log.info("=== MANUALLY ENABLING REORDER MODE ===");
		reorderMode = true;
		setDragState(DragState.ENABLED);
		rebuildFacilityRows(true);
		log.info("=== REORDER MODE ENABLED ===");
	}
	
	/**
	 * Disable reorder mode for testing
	 */
	public void disableReorderMode()
	{
		log.info("=== MANUALLY DISABLING REORDER MODE ===");
		reorderMode = false;
		setDragState(DragState.DISABLED);
		rebuildFacilityRows(false);
		log.info("=== REORDER MODE DISABLED ===");
	}
	
	/**
	 * Emergency method to completely disable all repositioning
	 */
	public void emergencyDisable()
	{
		log.info("=== EMERGENCY DISABLE - STOPPING ALL REPOSITIONING ===");
		reorderMode = false;
		needsReordering = false;
		setDragState(DragState.DISABLED);
		
		// Clear any saved configuration that might be causing issues
		configManager.unsetConfiguration(CONFIG_GROUP, CONFIG_KEY);
		
		log.info("=== EMERGENCY DISABLE COMPLETE - RESTART INTERFACE ===");
	}
	
	/**
	 * Enable comprehensive debugging - logs all script activity and widget changes
	 */
	private boolean debugMode = false;
	
	public void enableDebugMode()
	{
		debugMode = true;
		log.info("=== DEBUG MODE ENABLED - Will log all script activity ===");
	}
	
	public void disableDebugMode()
	{
		debugMode = false;
		log.info("=== DEBUG MODE DISABLED ===");
	}
	
	// Static reference for console access
	private static SailingInterfaceRepositioner instance;
	
	@Override
	public void startUp()
	{
		instance = this;
		initializeFacilityRows();
		loadRowOrder();
		updateReorderMode();
	}
	
	@Override
	public void shutDown()
	{
		instance = null;
		reorderMode = false;
		setDragState(DragState.DISABLED);
	}
	
	/**
	 * Get the current instance for console access
	 * Usage: SailingInterfaceRepositioner.getInstance().testMoveChumToBottom()
	 */
	public static SailingInterfaceRepositioner getInstance()
	{
		return instance;
	}

	/**
	 * Test method: Move Chum station to the bottom programmatically
	 * This bypasses drag and drop to test direct widget positioning
	 */
	public void testMoveChumToBottom()
	{
		log.info("=== TESTING: MOVE CHUM TO BOTTOM ===");
		
		Widget facilitiesWidget = client.getWidget(FACILITIES_ROWS_WIDGET_ID);
		if (facilitiesWidget == null)
		{
			log.error("Facilities widget not found!");
			return;
		}
		
		Widget[] children = facilitiesWidget.getChildren();
		if (children == null || children.length == 0)
		{
			log.error("No children found in facilities widget!");
			return;
		}
		
		log.info("Current facility order: {}", facilityRows.stream().map(FacilityRow::getName).toArray());
		
		// Find Chum row
		FacilityRow chumRow = facilityRows.stream()
			.filter(row -> "Chum".equals(row.getName()))
			.findFirst()
			.orElse(null);
		
		if (chumRow == null)
		{
			log.error("Chum row not found!");
			return;
		}
		
		log.info("Found Chum row: indices {}-{}", chumRow.getStartIndex(), chumRow.getEndIndex());
		
		// Get current Y positions of all rows for reference
		log.info("Current Y positions:");
		for (FacilityRow row : facilityRows)
		{
			if (row.getStartIndex() < children.length && children[row.getStartIndex()] != null)
			{
				int currentY = children[row.getStartIndex()].getOriginalY();
				log.info("  {} row: Y={}", row.getName(), currentY);
			}
		}
		
		// Move Chum to bottom: reorder the facility rows list
		List<FacilityRow> newOrder = new ArrayList<>();
		
		// Add all rows except Chum
		for (FacilityRow row : facilityRows)
		{
			if (!"Chum".equals(row.getName()))
			{
				newOrder.add(row);
			}
		}
		
		// Add Chum at the end
		newOrder.add(chumRow);
		
		// Update the facility rows list
		facilityRows.clear();
		facilityRows.addAll(newOrder);
		
		log.info("New facility order: {}", facilityRows.stream().map(FacilityRow::getName).toArray());
		
		// Apply the new positioning
		applyRowOrder();
		
		// Log new Y positions
		log.info("New Y positions after move:");
		for (FacilityRow row : facilityRows)
		{
			if (row.getStartIndex() < children.length && children[row.getStartIndex()] != null)
			{
				int newY = children[row.getStartIndex()].getOriginalY();
				log.info("  {} row: Y={}", row.getName(), newY);
			}
		}
		
		log.info("=== CHUM MOVE TEST COMPLETE ===");
	}
	
	/**
	 * Test method: Reset to default row order
	 */
	public void testResetToDefault()
	{
		log.info("=== TESTING: RESET TO DEFAULT ORDER ===");
		
		// Reset to default order
		initializeFacilityRows();
		
		log.info("Reset to default order: {}", facilityRows.stream().map(FacilityRow::getName).toArray());
		
		// Apply the default positioning
		applyRowOrder();
		
		log.info("=== RESET TO DEFAULT COMPLETE ===");
	}
	
	/**
	 * Test method: Custom order - Helm, Repairs, Net One, Net Two, Boosts, Chum
	 * This puts Chum at the very bottom with Boosts just above it
	 */
	public void testCustomOrder()
	{
		log.info("=== TESTING: CUSTOM ORDER (Helm, Repairs, Net One, Net Two, Boosts, Chum) ===");
		
		Widget facilitiesWidget = client.getWidget(FACILITIES_ROWS_WIDGET_ID);
		if (facilitiesWidget == null)
		{
			log.error("Facilities widget not found!");
			return;
		}
		
		Widget[] children = facilitiesWidget.getChildren();
		if (children == null || children.length == 0)
		{
			log.error("No children found in facilities widget!");
			return;
		}
		
		log.info("Current facility order: {}", facilityRows.stream().map(FacilityRow::getName).toArray());
		
		// Log current Y positions
		log.info("Current Y positions:");
		for (FacilityRow row : facilityRows)
		{
			if (row.getStartIndex() < children.length && children[row.getStartIndex()] != null)
			{
				int currentY = children[row.getStartIndex()].getOriginalY();
				log.info("  {} row: Y={}", row.getName(), currentY);
			}
		}
		
		// Create the custom order: Helm, Repairs, Net One, Net Two, Boosts, Chum
		List<FacilityRow> customOrder = new ArrayList<>();
		
		// Add fixed rows first (Helm and Repairs)
		customOrder.add(new FacilityRow("Helm", HELM_ROW_START, HELM_ROW_END));
		customOrder.add(new FacilityRow("Repairs", REPAIRS_ROW_START, REPAIRS_ROW_END));
		
		// Add draggable rows in custom order: Net One, Net Two, Boosts, Chum
		customOrder.add(new FacilityRow("Net One", NET_ONE_ROW_START, NET_ONE_ROW_END));
		customOrder.add(new FacilityRow("Net Two", NET_TWO_ROW_START, NET_TWO_ROW_END));
		customOrder.add(new FacilityRow("Boosts", BOOSTS_ROW_START, BOOSTS_ROW_END));
		customOrder.add(new FacilityRow("Chum", CHUM_ROW_START, CHUM_ROW_END));
		
		// Update the facility rows list
		facilityRows.clear();
		facilityRows.addAll(customOrder);
		
		log.info("New facility order: {}", facilityRows.stream().map(FacilityRow::getName).toArray());
		
		// Apply the new positioning
		applyRowOrder();
		
		// Log new Y positions
		log.info("New Y positions after custom reorder:");
		for (FacilityRow row : facilityRows)
		{
			if (row.getStartIndex() < children.length && children[row.getStartIndex()] != null)
			{
				int newY = children[row.getStartIndex()].getOriginalY();
				log.info("  {} row: Y={}", row.getName(), newY);
			}
		}
		
		log.info("=== CUSTOM ORDER TEST COMPLETE ===");
		log.info("Expected visual order from top to bottom: Helm → Repairs → Net One → Net Two → Boosts → Chum");
	}
	
	/**
	 * Calculate the child index of a widget within the facilities interface
	 */
	private int calculateChildIndex(int widgetId)
	{
		Widget facilitiesWidget = client.getWidget(FACILITIES_ROWS_WIDGET_ID);
		if (facilitiesWidget == null)
		{
			return -1;
		}
		
		Widget[] children = facilitiesWidget.getChildren();
		if (children == null)
		{
			return -1;
		}
		
		// Find the widget index in the children array
		for (int i = 0; i < children.length; i++)
		{
			if (children[i] != null && children[i].getId() == widgetId)
			{
				return i;
			}
		}
		
		return -1;
	}
	
	/**
	 * Comprehensive diagnostic method to identify drag configuration issues
	 */
	public void runDragDiagnostics()
	{
		log.info("=== RUNNING COMPREHENSIVE DRAG DIAGNOSTICS ===");
		
		// 1. Check basic state
		log.info("1. Basic State Check:");
		log.info("   Reorder mode: {}", reorderMode);
		log.info("   Drag state: {}", currentDragState);
		log.info("   Sailing interface open: {}", isSailingInterfaceOpen());
		
		// 2. Check widget availability
		log.info("2. Widget Availability:");
		Widget facilitiesWidget = client.getWidget(FACILITIES_ROWS_WIDGET_ID);
		if (facilitiesWidget == null)
		{
			log.error("   PROBLEM: Facilities widget not found! (ID: 0x{})", Integer.toHexString(FACILITIES_ROWS_WIDGET_ID));
			log.info("=== DIAGNOSTICS FAILED - WIDGET NOT FOUND ===");
			return;
		}
		
		log.info("   Facilities widget: FOUND (ID: 0x{}, hidden: {})", 
			Integer.toHexString(facilitiesWidget.getId()), facilitiesWidget.isHidden());
		
		Widget[] children = facilitiesWidget.getChildren();
		if (children == null)
		{
			log.error("   PROBLEM: No children in facilities widget!");
			log.info("=== DIAGNOSTICS FAILED - NO CHILDREN ===");
			return;
		}
		
		log.info("   Children count: {}", children.length);
		
		// 3. Check facility row configuration
		log.info("3. Facility Row Configuration:");
		for (FacilityRow row : facilityRows)
		{
			log.info("   Row: {} (indices {}-{})", row.getName(), row.getStartIndex(), row.getEndIndex());
			
			// Check if indices are valid
			if (row.getStartIndex() >= children.length || row.getEndIndex() >= children.length)
			{
				log.error("     PROBLEM: Row indices exceed children array length!");
				continue;
			}
			
			// Check first few widgets in each row
			int validWidgets = 0;
			int dragEnabledWidgets = 0;
			
			for (int i = row.getStartIndex(); i <= Math.min(row.getStartIndex() + 2, row.getEndIndex()) && i < children.length; i++)
			{
				Widget widget = children[i];
				if (widget != null)
				{
					validWidgets++;
					int config = widget.getClickMask();
					boolean hasDrag = (config & DRAG) != 0;
					boolean hasDragOn = (config & DRAG_ON) != 0;
					
					if (hasDrag && hasDragOn)
					{
						dragEnabledWidgets++;
					}
					
					log.info("     Widget {}: ID=0x{}, config=0x{}, DRAG={}, DRAG_ON={}", 
						i, Integer.toHexString(widget.getId()), Integer.toHexString(config), hasDrag, hasDragOn);
				}
			}
			
			log.info("     Summary: {}/{} widgets valid, {}/{} drag-enabled", 
				validWidgets, Math.min(3, row.getEndIndex() - row.getStartIndex() + 1), 
				dragEnabledWidgets, validWidgets);
		}
		
		// 4. Test drag configuration
		log.info("4. Testing Drag Configuration:");
		if (reorderMode)
		{
			log.info("   Reorder mode is ON - widgets should have drag flags");
		}
		else
		{
			log.info("   Reorder mode is OFF - widgets should NOT have drag flags");
		}
		
		// 5. Recommendations
		log.info("5. Recommendations:");
		if (!isSailingInterfaceOpen())
		{
			log.warn("   - Open the sailing interface first");
		}
		if (!reorderMode)
		{
			log.warn("   - Enable reorder mode in sailing plugin config");
		}
		
		log.info("=== DRAG DIAGNOSTICS COMPLETE ===");
	}
}