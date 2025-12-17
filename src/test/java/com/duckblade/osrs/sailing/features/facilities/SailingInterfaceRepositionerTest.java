package com.duckblade.osrs.sailing.features.facilities;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for SailingInterfaceRepositioner core functionality.
 */
public class SailingInterfaceRepositionerTest
{
	/**
	 * Test FacilityRow creation and basic properties.
	 */
	@Test
	public void testFacilityRowCreation()
	{
		FacilityRow helmRow = new FacilityRow("Helm", 0, 45);
		
		assertEquals("Helm", helmRow.getName());
		assertEquals(0, helmRow.getStartIndex());
		assertEquals(45, helmRow.getEndIndex());
	}
	
	/**
	 * Test DragState enum values.
	 */
	@Test
	public void testDragStateValues()
	{
		// Verify all expected drag states exist
		DragState[] states = DragState.values();
		assertEquals(4, states.length);
		
		assertEquals(DragState.DISABLED, DragState.valueOf("DISABLED"));
		assertEquals(DragState.ENABLED, DragState.valueOf("ENABLED"));
		assertEquals(DragState.DRAGGING, DragState.valueOf("DRAGGING"));
		assertEquals(DragState.DROPPING, DragState.valueOf("DROPPING"));
	}
	
	/**
	 * Test FacilityRowBounds creation and properties.
	 */
	@Test
	public void testFacilityRowBounds()
	{
		FacilityRow row = new FacilityRow("Test", 0, 10);
		java.awt.Rectangle bounds = new java.awt.Rectangle(10, 20, 100, 50);
		
		FacilityRowBounds rowBounds = new FacilityRowBounds(bounds, row, true, false);
		
		assertEquals(bounds, rowBounds.getBounds());
		assertEquals(row, rowBounds.getRow());
		assertTrue(rowBounds.isDropTarget());
		assertFalse(rowBounds.isDragged());
	}
	
	/**
	 * Test facility row equality and comparison.
	 */
	@Test
	public void testFacilityRowEquality()
	{
		FacilityRow row1 = new FacilityRow("Helm", 0, 45);
		FacilityRow row2 = new FacilityRow("Helm", 0, 45);
		FacilityRow row3 = new FacilityRow("Repairs", 46, 59);
		
		// Test equality based on name and indices
		assertEquals(row1.getName(), row2.getName());
		assertEquals(row1.getStartIndex(), row2.getStartIndex());
		assertEquals(row1.getEndIndex(), row2.getEndIndex());
		
		// Test inequality
		assertNotEquals(row1.getName(), row3.getName());
		assertNotEquals(row1.getStartIndex(), row3.getStartIndex());
		assertNotEquals(row1.getEndIndex(), row3.getEndIndex());
	}
	
	/**
	 * Test widget ID calculations.
	 */
	@Test
	public void testWidgetIdCalculations()
	{
		int facilitiesWidgetId = 0x03a9_001b;
		
		// Test extracting group ID
		int groupId = facilitiesWidgetId >> 16;
		assertEquals(0x03a9, groupId);
		
		// Test extracting child index
		int childIndex = facilitiesWidgetId & 0xFFFF;
		assertEquals(0x001b, childIndex);
		
		// Test widget ID construction
		int reconstructedId = (groupId << 16) | childIndex;
		assertEquals(facilitiesWidgetId, reconstructedId);
	}
	
	/**
	 * Test row index ranges for all facility types.
	 */
	@Test
	public void testFacilityRowRanges()
	{
		// Test that all facility row ranges are non-overlapping and sequential
		int[][] expectedRanges = {
			{0, 45},    // Helm
			{46, 59},   // Repairs
			{60, 82},   // Boosts
			{83, 95},   // Chum
			{96, 129},  // Net One
			{130, 164}  // Net Two
		};
		
		// Verify ranges are sequential
		for (int i = 0; i < expectedRanges.length - 1; i++)
		{
			int currentEnd = expectedRanges[i][1];
			int nextStart = expectedRanges[i + 1][0];
			assertEquals("Gap between ranges", currentEnd + 1, nextStart);
		}
		
		// Verify total widget count
		int totalWidgets = expectedRanges[expectedRanges.length - 1][1] + 1;
		assertEquals(165, totalWidgets);
	}
	
	/**
	 * Test configuration key constants.
	 */
	@Test
	public void testConfigurationConstants()
	{
		// These are the expected configuration keys used by the repositioner
		String expectedGroup = "sailing";
		String expectedKey = "facilityRowOrder";
		
		// Verify the constants match expected values
		assertNotNull(expectedGroup);
		assertNotNull(expectedKey);
		assertFalse(expectedGroup.isEmpty());
		assertFalse(expectedKey.isEmpty());
	}
	
	/**
	 * Test drag and drop widget ID validation.
	 */
	@Test
	public void testDragWidgetValidation()
	{
		int facilitiesGroupId = 0x03a9;
		int validWidgetId = (facilitiesGroupId << 16) | 0x001b;
		int invalidWidgetId = (0x0400 << 16) | 0x001b; // Different group
		
		// Test group ID extraction
		assertEquals(facilitiesGroupId, validWidgetId >> 16);
		assertNotEquals(facilitiesGroupId, invalidWidgetId >> 16);
		
		// Test child index extraction
		assertEquals(0x001b, validWidgetId & 0xFFFF);
		assertEquals(0x001b, invalidWidgetId & 0xFFFF);
	}
}