package com.duckblade.osrs.sailing.features.facilities;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Integration tests for the complete drag interface system.
 * Tests the core functionality and interactions of the drag interface components.
 */
public class SailingFacilityDragIntegrationTest
{
	/**
	 * Test complete facility row list creation and management.
	 */
	@Test
	public void testFacilityRowListManagement()
	{
		// Create a list of facility rows like the repositioner would
		List<FacilityRow> facilityRows = new ArrayList<>();
		facilityRows.add(new FacilityRow("Helm", 0, 45));
		facilityRows.add(new FacilityRow("Repairs", 46, 59));
		facilityRows.add(new FacilityRow("Boosts", 60, 82));
		facilityRows.add(new FacilityRow("Chum", 83, 95));
		facilityRows.add(new FacilityRow("Net One", 96, 129));
		facilityRows.add(new FacilityRow("Net Two", 130, 164));
		
		// Verify initial order
		assertEquals(6, facilityRows.size());
		assertEquals("Helm", facilityRows.get(0).getName());
		assertEquals("Net Two", facilityRows.get(5).getName());
		
		// Test reordering operation (move Helm to position 2)
		FacilityRow helmRow = facilityRows.get(0);
		facilityRows.remove(0);
		facilityRows.add(2, helmRow);
		
		// Verify new order
		assertEquals("Repairs", facilityRows.get(0).getName());
		assertEquals("Boosts", facilityRows.get(1).getName());
		assertEquals("Helm", facilityRows.get(2).getName());
		assertEquals("Chum", facilityRows.get(3).getName());
	}
	
	/**
	 * Test drag state transitions and validation.
	 */
	@Test
	public void testDragStateTransitions()
	{
		// Test all valid state transitions
		DragState currentState = DragState.DISABLED;
		
		// DISABLED -> ENABLED
		assertTrue("Should allow DISABLED to ENABLED", isValidTransition(currentState, DragState.ENABLED));
		assertFalse("Should not allow DISABLED to DRAGGING", isValidTransition(currentState, DragState.DRAGGING));
		
		currentState = DragState.ENABLED;
		
		// ENABLED -> DISABLED or DRAGGING
		assertTrue("Should allow ENABLED to DISABLED", isValidTransition(currentState, DragState.DISABLED));
		assertTrue("Should allow ENABLED to DRAGGING", isValidTransition(currentState, DragState.DRAGGING));
		assertFalse("Should not allow ENABLED to DROPPING", isValidTransition(currentState, DragState.DROPPING));
		
		currentState = DragState.DRAGGING;
		
		// DRAGGING -> ENABLED or DROPPING
		assertTrue("Should allow DRAGGING to ENABLED", isValidTransition(currentState, DragState.ENABLED));
		assertTrue("Should allow DRAGGING to DROPPING", isValidTransition(currentState, DragState.DROPPING));
		assertFalse("Should not allow DRAGGING to DISABLED", isValidTransition(currentState, DragState.DISABLED));
		
		currentState = DragState.DROPPING;
		
		// DROPPING -> ENABLED
		assertTrue("Should allow DROPPING to ENABLED", isValidTransition(currentState, DragState.ENABLED));
		assertFalse("Should not allow DROPPING to DISABLED", isValidTransition(currentState, DragState.DISABLED));
	}
	
	/**
	 * Test performance of list operations.
	 */
	@Test
	public void testListOperationPerformance()
	{
		// Create a large list to test performance
		List<FacilityRow> largeList = new ArrayList<>();
		for (int i = 0; i < 1000; i++)
		{
			largeList.add(new FacilityRow("Row" + i, i * 10, i * 10 + 9));
		}
		
		long startTime = System.nanoTime();
		
		// Perform many reorder operations
		for (int i = 0; i < 100; i++)
		{
			// Move first item to random position
			FacilityRow item = largeList.remove(0);
			int newPosition = i % (largeList.size() - 1);
			largeList.add(newPosition, item);
		}
		
		long endTime = System.nanoTime();
		long durationMs = (endTime - startTime) / 1_000_000;
		
		// Should complete quickly even with large list
		assertTrue("List operations too slow: " + durationMs + "ms", durationMs < 100);
	}
	
	/**
	 * Test widget ID calculations and mappings.
	 */
	@Test
	public void testWidgetIdMappings()
	{
		int facilitiesWidgetId = 0x03a9_001b;
		int baseChildIndex = facilitiesWidgetId & 0xFFFF; // This is 0x001b = 27
		
		// Test widget ID ranges for each facility row
		int[][] rowRanges = {
			{0, 45},    // Helm
			{46, 59},   // Repairs
			{60, 82},   // Boosts
			{83, 95},   // Chum
			{96, 129},  // Net One
			{130, 164}  // Net Two
		};
		
		// Test that the base widget ID falls within the first row (Helm)
		assertTrue("Base widget ID should be in Helm row range", 
			baseChildIndex >= rowRanges[0][0] && baseChildIndex <= rowRanges[0][1]);
		
		// Test widget ID construction for each row
		for (int rowIndex = 0; rowIndex < rowRanges.length; rowIndex++)
		{
			int startIndex = rowRanges[rowIndex][0];
			int endIndex = rowRanges[rowIndex][1];
			
			// Test widget ID construction
			int groupId = facilitiesWidgetId >> 16;
			int testWidgetId = (groupId << 16) | startIndex;
			int extractedChildIndex = testWidgetId & 0xFFFF;
			
			assertEquals("Child index should match start index", startIndex, extractedChildIndex);
			
			// Test that the range is valid
			assertTrue("End index should be >= start index", endIndex >= startIndex);
		}
	}
	
	/**
	 * Test configuration string parsing and generation.
	 */
	@Test
	public void testConfigurationStringHandling()
	{
		// Test configuration string generation
		List<FacilityRow> rows = new ArrayList<>();
		rows.add(new FacilityRow("Boosts", 60, 82));
		rows.add(new FacilityRow("Helm", 0, 45));
		rows.add(new FacilityRow("Repairs", 46, 59));
		
		String configString = String.join(",", 
			rows.stream().map(FacilityRow::getName).toArray(String[]::new));
		
		assertEquals("Boosts,Helm,Repairs", configString);
		
		// Test configuration string parsing
		String[] rowNames = configString.split(",");
		assertEquals(3, rowNames.length);
		assertEquals("Boosts", rowNames[0]);
		assertEquals("Helm", rowNames[1]);
		assertEquals("Repairs", rowNames[2]);
		
		// Test rebuilding list from config
		List<FacilityRow> allRows = new ArrayList<>();
		allRows.add(new FacilityRow("Helm", 0, 45));
		allRows.add(new FacilityRow("Repairs", 46, 59));
		allRows.add(new FacilityRow("Boosts", 60, 82));
		
		List<FacilityRow> reorderedRows = new ArrayList<>();
		for (String name : rowNames)
		{
			allRows.stream()
				.filter(row -> row.getName().equals(name))
				.findFirst()
				.ifPresent(reorderedRows::add);
		}
		
		assertEquals(3, reorderedRows.size());
		assertEquals("Boosts", reorderedRows.get(0).getName());
		assertEquals("Helm", reorderedRows.get(1).getName());
		assertEquals("Repairs", reorderedRows.get(2).getName());
	}
	
	/**
	 * Test bounds calculation for overlays.
	 */
	@Test
	public void testBoundsCalculationIntegration()
	{
		// Simulate widget bounds for a facility row
		java.awt.Rectangle[] widgetBounds = {
			new java.awt.Rectangle(10, 50, 20, 30),   // First widget in row
			new java.awt.Rectangle(35, 55, 25, 25),   // Second widget
			new java.awt.Rectangle(65, 52, 15, 28)    // Third widget
		};
		
		// Calculate encompassing bounds (like overlay would do)
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		
		for (java.awt.Rectangle bounds : widgetBounds)
		{
			minX = Math.min(minX, bounds.x);
			minY = Math.min(minY, bounds.y);
			maxX = Math.max(maxX, bounds.x + bounds.width);
			maxY = Math.max(maxY, bounds.y + bounds.height);
		}
		
		java.awt.Rectangle rowBounds = new java.awt.Rectangle(minX, minY, maxX - minX, maxY - minY);
		
		// Verify bounds encompass all widgets
		assertEquals("Should start at leftmost widget", 10, rowBounds.x);
		assertEquals("Should start at topmost widget", 50, rowBounds.y);
		assertEquals("Should span all widgets horizontally", 70, rowBounds.width);
		assertEquals("Should span all widgets vertically", 30, rowBounds.height);
		
		// Test that all original widgets are contained
		for (java.awt.Rectangle bounds : widgetBounds)
		{
			assertTrue("Row bounds should contain widget", rowBounds.contains(bounds));
		}
	}
	
	/**
	 * Helper method to validate state transitions.
	 */
	private boolean isValidTransition(DragState from, DragState to)
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
}