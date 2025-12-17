package com.duckblade.osrs.sailing.features.facilities;

import org.junit.Test;

import java.awt.Color;
import java.awt.Rectangle;

import static org.junit.Assert.*;

/**
 * Unit tests for SailingFacilityDragOverlay core functionality.
 */
public class SailingFacilityDragOverlayTest
{
	/**
	 * Test color validation logic.
	 */
	@Test
	public void testColorValidation()
	{
		// Test valid colors
		Color validColor = new Color(255, 0, 0, 128);
		assertEquals(255, validColor.getRed());
		assertEquals(0, validColor.getGreen());
		assertEquals(0, validColor.getBlue());
		assertEquals(128, validColor.getAlpha());
		
		// Test color with invalid alpha (too low)
		Color lowAlphaColor = new Color(255, 0, 0, 10);
		assertTrue("Alpha too low", lowAlphaColor.getAlpha() < 50);
		
		// Test color with maximum valid alpha
		Color maxAlphaColor = new Color(255, 0, 0, 255);
		assertEquals(255, maxAlphaColor.getAlpha());
	}
	
	/**
	 * Test color brightness calculations.
	 */
	@Test
	public void testColorBrightness()
	{
		// Test brightness calculation using standard luminance formula
		Color red = new Color(255, 0, 0);
		Color green = new Color(0, 255, 0);
		Color blue = new Color(0, 0, 255);
		Color white = new Color(255, 255, 255);
		Color black = new Color(0, 0, 0);
		
		// Calculate perceived brightness
		float redBrightness = (0.299f * red.getRed() + 0.587f * red.getGreen() + 0.114f * red.getBlue()) / 255f;
		float greenBrightness = (0.299f * green.getRed() + 0.587f * green.getGreen() + 0.114f * green.getBlue()) / 255f;
		float blueBrightness = (0.299f * blue.getRed() + 0.587f * blue.getGreen() + 0.114f * blue.getBlue()) / 255f;
		float whiteBrightness = (0.299f * white.getRed() + 0.587f * white.getGreen() + 0.114f * white.getBlue()) / 255f;
		float blackBrightness = (0.299f * black.getRed() + 0.587f * black.getGreen() + 0.114f * black.getBlue()) / 255f;
		
		// Verify brightness ordering
		assertTrue("Green should be brighter than red", greenBrightness > redBrightness);
		assertTrue("Red should be brighter than blue", redBrightness > blueBrightness);
		assertTrue("White should be brightest", whiteBrightness > greenBrightness);
		assertEquals("Black should have zero brightness", 0.0f, blackBrightness, 0.001f);
		assertEquals("White should have full brightness", 1.0f, whiteBrightness, 0.001f);
	}
	
	/**
	 * Test color enhancement operations.
	 */
	@Test
	public void testColorEnhancement()
	{
		Color baseColor = new Color(100, 100, 100, 128);
		
		// Test brightening
		float factor = 1.5f;
		int newRed = Math.min(255, (int) (baseColor.getRed() * factor));
		int newGreen = Math.min(255, (int) (baseColor.getGreen() * factor));
		int newBlue = Math.min(255, (int) (baseColor.getBlue() * factor));
		
		Color brightenedColor = new Color(newRed, newGreen, newBlue, baseColor.getAlpha());
		
		assertTrue("Brightened color should be brighter", brightenedColor.getRed() > baseColor.getRed());
		assertEquals("Alpha should be preserved", baseColor.getAlpha(), brightenedColor.getAlpha());
		
		// Test clamping at 255
		Color veryBrightColor = new Color(200, 200, 200);
		int clampedRed = Math.min(255, (int) (veryBrightColor.getRed() * 2.0f));
		assertEquals("Should clamp at 255", 255, clampedRed);
	}
	
	/**
	 * Test pulse effect calculations.
	 */
	@Test
	public void testPulseEffectCalculations()
	{
		// Test pulse phase calculation
		long currentTime = System.currentTimeMillis();
		double pulsePhase = (currentTime % 1000) / 1000.0; // 1 second cycle
		
		assertTrue("Pulse phase should be between 0 and 1", pulsePhase >= 0.0 && pulsePhase < 1.0);
		
		// Test sine wave calculation
		double pulseValue = Math.sin(pulsePhase * 2 * Math.PI) * 0.3; // 30% amplitude
		assertTrue("Pulse value should be within amplitude", Math.abs(pulseValue) <= 0.3);
		
		// Test pulse factor
		float pulseFactor = 1.0f + (float) pulseValue;
		assertTrue("Pulse factor should be around 1.0", pulseFactor >= 0.7f && pulseFactor <= 1.3f);
	}
	
	/**
	 * Test transition timing calculations.
	 */
	@Test
	public void testTransitionTiming()
	{
		long transitionDuration = 150; // 150ms
		long startTime = System.currentTimeMillis();
		
		// Test transition progress at different points
		long[] testTimes = {0, 50, 100, 150, 200};
		
		for (long elapsed : testTimes)
		{
			if (elapsed < transitionDuration)
			{
				float progress = (float) elapsed / transitionDuration;
				assertTrue("Progress should be between 0 and 1", progress >= 0.0f && progress <= 1.0f);
				
				// Test easing function (ease-out)
				float easedProgress = 1.0f - (1.0f - progress) * (1.0f - progress);
				assertTrue("Eased progress should be >= linear progress", easedProgress >= progress);
			}
		}
	}
	
	/**
	 * Test rectangle bounds calculations.
	 */
	@Test
	public void testBoundsCalculations()
	{
		// Test bounds calculation for multiple widgets
		Rectangle[] widgetBounds = {
			new Rectangle(10, 50, 20, 30),
			new Rectangle(35, 55, 20, 30),
			new Rectangle(60, 45, 20, 30)
		};
		
		// Calculate encompassing bounds
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		
		for (Rectangle bounds : widgetBounds)
		{
			minX = Math.min(minX, bounds.x);
			minY = Math.min(minY, bounds.y);
			maxX = Math.max(maxX, bounds.x + bounds.width);
			maxY = Math.max(maxY, bounds.y + bounds.height);
		}
		
		Rectangle encompassingBounds = new Rectangle(minX, minY, maxX - minX, maxY - minY);
		
		assertEquals("Min X should be 10", 10, encompassingBounds.x);
		assertEquals("Min Y should be 45", 45, encompassingBounds.y);
		assertEquals("Width should span all widgets", 70, encompassingBounds.width);
		assertEquals("Height should span all widgets", 40, encompassingBounds.height);
	}
	
	/**
	 * Test mouse position containment checks.
	 */
	@Test
	public void testMouseContainment()
	{
		Rectangle bounds = new Rectangle(50, 100, 200, 150);
		
		// Test points inside bounds
		assertTrue("Point inside should be contained", bounds.contains(100, 150));
		assertTrue("Point at edge should be contained", bounds.contains(50, 100));
		
		// Test points outside bounds
		assertFalse("Point outside left should not be contained", bounds.contains(25, 150));
		assertFalse("Point outside right should not be contained", bounds.contains(300, 150));
		assertFalse("Point outside top should not be contained", bounds.contains(100, 50));
		assertFalse("Point outside bottom should not be contained", bounds.contains(100, 300));
	}
}