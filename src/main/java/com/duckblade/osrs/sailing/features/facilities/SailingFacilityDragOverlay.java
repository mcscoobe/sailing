package com.duckblade.osrs.sailing.features.facilities;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;

/**
 * Visual overlay for the sailing facility drag interface.
 * Provides colored outlines and drag state indicators similar to the prayer reordering system.
 * 
 * This overlay integrates with SailingInterfaceRepositioner to provide visual feedback
 * for drag-and-drop operations. The repositioner manages the logical state while this
 * overlay handles the visual representation.
 */
@Slf4j
@Singleton
public class SailingFacilityDragOverlay extends Overlay implements PluginLifecycleComponent
{
	private static final int FACILITIES_ROWS_WIDGET_ID = 0x03a9_001b;
	private static final int OUTLINE_STROKE_WIDTH = 2;
	private static final int HOVER_STROKE_WIDTH = 3;
	
	// Animation constants for smooth transitions
	private static final long TRANSITION_DURATION_MS = 150; // 150ms for smooth transitions
	private static final float PULSE_AMPLITUDE = 0.3f; // 30% brightness variation for pulse effect
	
	// Color validation constants
	private static final int MIN_ALPHA = 50; // Minimum alpha for visibility
	private static final int MAX_ALPHA = 255; // Maximum alpha value
	
	@Nonnull
	private final Client client;
	private final SailingConfig config;
	private final SailingInterfaceRepositioner repositioner;
	
	private Point mousePosition;
	private long lastStateChangeTime = 0;
	private DragState previousDragState = DragState.DISABLED;
	
	// Cached validated colors for performance and consistency
	private Color cachedOutlineColor;
	private Color cachedActiveColor;
	private Color cachedDropTargetColor;
	private Color cachedInvalidDropColor;
	private long lastColorCacheTime = 0;
	private static final long COLOR_CACHE_DURATION_MS = 100; // Cache colors for 100ms
	
	@Inject
	public SailingFacilityDragOverlay(
		@Nonnull Client client, 
		SailingConfig config, 
		SailingInterfaceRepositioner repositioner)
	{
		this.client = client;
		this.config = config;
		this.repositioner = repositioner;
		
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_HIGH);
	}
	
	@Override
	public boolean isEnabled(SailingConfig config)
	{
		// Only enable the overlay when reorder mode is enabled in config
		// The overlay will handle its own visibility based on drag state
		return config.reorderFacilityRows();
	}
	
	@Override
	public void startUp()
	{
		log.debug("SailingFacilityDragOverlay starting up");
		// Reset state on startup to ensure clean initialization
		mousePosition = null;
		lastStateChangeTime = System.currentTimeMillis();
		previousDragState = DragState.DISABLED;
		// Clear color cache to ensure fresh colors on startup
		clearColorCache();
	}
	
	@Override
	public void shutDown()
	{
		log.debug("SailingFacilityDragOverlay shutting down");
		// Clean up resources and reset state
		mousePosition = null;
		lastStateChangeTime = 0;
		previousDragState = DragState.DISABLED;
		// Clear color cache on shutdown
		clearColorCache();
	}
	
	@Override
	public Dimension render(Graphics2D graphics)
	{
		// Ensure overlay is only active when needed
		if (!config.reorderFacilityRows())
		{
			return null;
		}
		
		DragState currentState = repositioner.getCurrentDragState();
		
		// Track state changes for smooth transitions
		if (currentState != previousDragState)
		{
			lastStateChangeTime = System.currentTimeMillis();
			previousDragState = currentState;
		}
		
		// Only render when drag interface should be visible
		if (currentState == DragState.DISABLED)
		{
			return null;
		}
		
		// Check if sailing interface is visible
		Widget facilitiesWidget = client.getWidget(FACILITIES_ROWS_WIDGET_ID);
		if (facilitiesWidget == null || facilitiesWidget.isHidden())
		{
			return null;
		}
		
		// Update mouse position for hover detection
		updateMousePosition();
		
		// Calculate bounds for all facility rows
		List<FacilityRowBounds> rowBounds = calculateFacilityRowBounds();
		
		// Render visual indicators - show outlines when reorder mode is enabled
		renderReorderModeVisuals(graphics, rowBounds, currentState);
		
		return null;
	}
	
	/**
	 * Updates the current mouse position for hover detection.
	 */
	private void updateMousePosition()
	{
		mousePosition = client.getMouseCanvasPosition();
	}
	
	/**
	 * Calculates bounding rectangles for all facility rows.
	 */
	private List<FacilityRowBounds> calculateFacilityRowBounds()
	{
		List<FacilityRowBounds> bounds = new ArrayList<>();
		Widget facilitiesWidget = client.getWidget(FACILITIES_ROWS_WIDGET_ID);
		
		if (facilitiesWidget == null)
		{
			return bounds;
		}
		
		Widget[] children = facilitiesWidget.getChildren();
		if (children == null)
		{
			return bounds;
		}
		
		List<FacilityRow> facilityRows = repositioner.getFacilityRows();
		
		for (FacilityRow row : facilityRows)
		{
			Rectangle rowBounds = calculateRowBounds(children, row);
			if (rowBounds != null)
			{
				// In the new system, we don't track specific dragged rows
				// All rows are potential drop targets when reorder mode is enabled
				boolean isDropTarget = repositioner.isValidDropTarget(row);
				
				bounds.add(new FacilityRowBounds(rowBounds, row, isDropTarget, false));
			}
		}
		
		return bounds;
	}
	
	/**
	 * Calculates the bounding rectangle for a specific facility row.
	 */
	private Rectangle calculateRowBounds(Widget[] children, FacilityRow row)
	{
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		
		boolean foundAnyWidget = false;
		
		// Find bounds that encompass all widgets in this row
		for (int i = row.getStartIndex(); i <= row.getEndIndex() && i < children.length; i++)
		{
			Widget widget = children[i];
			if (widget != null && !widget.isHidden())
			{
				Rectangle widgetBounds = widget.getBounds();
				if (widgetBounds != null)
				{
					minX = Math.min(minX, widgetBounds.x);
					minY = Math.min(minY, widgetBounds.y);
					maxX = Math.max(maxX, widgetBounds.x + widgetBounds.width);
					maxY = Math.max(maxY, widgetBounds.y + widgetBounds.height);
					foundAnyWidget = true;
				}
			}
		}
		
		if (!foundAnyWidget)
		{
			return null;
		}
		
		return new Rectangle(minX, minY, maxX - minX, maxY - minY);
	}
	
	/**
	 * Determines if a row is a valid drop target for the current drag operation.
	 */
	private boolean isValidDropTarget(FacilityRow row, FacilityRow draggedRow)
	{
		// Use the repositioner's validation logic for consistency
		return repositioner.isValidDropTarget(row);
	}
	
	/**
	 * Renders visual indicators for reorder mode.
	 * Shows outlines around draggable facility rows when reorder mode is enabled.
	 */
	private void renderReorderModeVisuals(Graphics2D graphics, List<FacilityRowBounds> rowBounds, DragState dragState)
	{
		Stroke originalStroke = graphics.getStroke();
		
		for (FacilityRowBounds bounds : rowBounds)
		{
			renderRowVisual(graphics, bounds, dragState);
		}
		
		graphics.setStroke(originalStroke);
	}
	
	/**
	 * Renders visual indicators for a single facility row.
	 */
	private void renderRowVisual(Graphics2D graphics, FacilityRowBounds bounds, DragState dragState)
	{
		Rectangle rect = bounds.getBounds();
		Color color = determineRowColor(bounds, dragState);
		int strokeWidth = determineStrokeWidth(bounds, dragState);
		
		if (color != null)
		{
			graphics.setStroke(new BasicStroke(strokeWidth));
			graphics.setColor(color);
			graphics.drawRect(rect.x, rect.y, rect.width, rect.height);
		}
	}
	
	/**
	 * Determines the appropriate color for a facility row based on its state.
	 * Uses validated colors and applies consistent color scheme rules.
	 */
	private Color determineRowColor(FacilityRowBounds bounds, DragState dragState)
	{
		// Show hover effect when mouse is over a row
		if (isRowHovered(bounds))
		{
			// Apply hover enhancement to outline color
			Color baseColor = applyColorSchemeConsistency(getValidatedOutlineColor(), ColorRole.HOVER);
			return brightenColor(baseColor, 1.3f);
		}
		
		// Default draggable outline when reorder mode is enabled
		if (dragState == DragState.ENABLED)
		{
			Color baseColor = applyColorSchemeConsistency(getValidatedOutlineColor(), ColorRole.OUTLINE);
			return applyTransitionEffect(baseColor, dragState);
		}
		
		return null; // No visual indicator
	}
	
	/**
	 * Determines the appropriate stroke width for a facility row.
	 */
	private int determineStrokeWidth(FacilityRowBounds bounds, DragState dragState)
	{
		// Use thicker stroke for hover to provide clear feedback
		if (isRowHovered(bounds))
		{
			return HOVER_STROKE_WIDTH;
		}
		
		return OUTLINE_STROKE_WIDTH;
	}
	
	/**
	 * Checks if the mouse is currently hovering over a facility row.
	 */
	private boolean isRowHovered(FacilityRowBounds bounds)
	{
		if (mousePosition == null)
		{
			return false;
		}
		
		Rectangle rect = bounds.getBounds();
		return rect.contains(mousePosition.getX(), mousePosition.getY());
	}
	
	/**
	 * Creates a brighter version of the given color.
	 */
	private Color brightenColor(Color color, float factor)
	{
		int red = Math.min(255, (int) (color.getRed() * factor));
		int green = Math.min(255, (int) (color.getGreen() * factor));
		int blue = Math.min(255, (int) (color.getBlue() * factor));
		return new Color(red, green, blue, color.getAlpha());
	}
	
	/**
	 * Applies a subtle pulse effect to colors for enhanced visual feedback.
	 * The pulse effect helps draw attention to important states like drag targets.
	 */
	private Color applyPulseEffect(Color baseColor, DragState dragState)
	{
		// Only apply pulse during active drag operations
		if (dragState != DragState.DRAGGING)
		{
			return baseColor;
		}
		
		// Calculate pulse factor based on time (creates a breathing effect)
		long currentTime = System.currentTimeMillis();
		double pulsePhase = (currentTime % 1000) / 1000.0; // 1 second cycle
		double pulseValue = Math.sin(pulsePhase * 2 * Math.PI) * PULSE_AMPLITUDE;
		float pulseFactor = 1.0f + (float) pulseValue;
		
		return brightenColor(baseColor, pulseFactor);
	}
	
	/**
	 * Applies smooth transition effects when entering or leaving drag states.
	 * This provides visual continuity during state changes.
	 */
	private Color applyTransitionEffect(Color baseColor, DragState dragState)
	{
		long timeSinceChange = System.currentTimeMillis() - lastStateChangeTime;
		
		// Apply fade-in effect for the first 150ms after state change
		if (timeSinceChange < TRANSITION_DURATION_MS)
		{
			float transitionProgress = (float) timeSinceChange / TRANSITION_DURATION_MS;
			// Smooth easing function (ease-out)
			transitionProgress = 1.0f - (1.0f - transitionProgress) * (1.0f - transitionProgress);
			
			// Fade from transparent to full opacity
			int alpha = (int) (baseColor.getAlpha() * transitionProgress);
			return new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alpha);
		}
		
		return baseColor;
	}
	
	/**
	 * Clears the color cache to force refresh of colors from configuration.
	 * This should be called when color settings change to ensure immediate updates.
	 */
	private void clearColorCache()
	{
		cachedOutlineColor = null;
		cachedActiveColor = null;
		cachedDropTargetColor = null;
		cachedInvalidDropColor = null;
		lastColorCacheTime = 0;
	}
	
	/**
	 * Gets the validated outline color for draggable facility rows.
	 * Colors are cached for performance and validated for visibility.
	 */
	private Color getValidatedOutlineColor()
	{
		refreshColorCacheIfNeeded();
		if (cachedOutlineColor == null)
		{
			cachedOutlineColor = validateAndApplyColor(config.facilityDragOutlineColor(), "outline");
		}
		return cachedOutlineColor;
	}
	
	/**
	 * Gets the validated active drag color for currently dragged rows.
	 * Colors are cached for performance and validated for visibility.
	 */
	private Color getValidatedActiveColor()
	{
		refreshColorCacheIfNeeded();
		if (cachedActiveColor == null)
		{
			cachedActiveColor = validateAndApplyColor(config.facilityDragActiveColor(), "active drag");
		}
		return cachedActiveColor;
	}
	
	/**
	 * Gets the validated drop target color for valid drop positions.
	 * Colors are cached for performance and validated for visibility.
	 */
	private Color getValidatedDropTargetColor()
	{
		refreshColorCacheIfNeeded();
		if (cachedDropTargetColor == null)
		{
			cachedDropTargetColor = validateAndApplyColor(config.facilityDropTargetColor(), "drop target");
		}
		return cachedDropTargetColor;
	}
	
	/**
	 * Gets the validated invalid drop color for invalid drop areas.
	 * Colors are cached for performance and validated for visibility.
	 */
	private Color getValidatedInvalidDropColor()
	{
		refreshColorCacheIfNeeded();
		if (cachedInvalidDropColor == null)
		{
			cachedInvalidDropColor = validateAndApplyColor(config.facilityInvalidDropColor(), "invalid drop");
		}
		return cachedInvalidDropColor;
	}
	
	/**
	 * Refreshes the color cache if it has expired.
	 * This ensures color changes are picked up within a reasonable time frame.
	 */
	private void refreshColorCacheIfNeeded()
	{
		long currentTime = System.currentTimeMillis();
		if (currentTime - lastColorCacheTime > COLOR_CACHE_DURATION_MS)
		{
			clearColorCache();
			lastColorCacheTime = currentTime;
		}
	}
	
	/**
	 * Validates and applies consistency rules to a color configuration value.
	 * Ensures colors meet minimum visibility requirements and logs warnings for invalid values.
	 * 
	 * @param configColor The color from configuration
	 * @param colorType The type of color for logging purposes
	 * @return A validated color that meets visibility requirements
	 */
	private Color validateAndApplyColor(Color configColor, String colorType)
	{
		if (configColor == null)
		{
			log.warn("Null {} color configuration, using default cyan", colorType);
			return new Color(0, 255, 255, 128); // Default cyan with 50% alpha
		}
		
		// Validate alpha for visibility
		int alpha = configColor.getAlpha();
		if (alpha < MIN_ALPHA)
		{
			log.warn("{} color alpha ({}) below minimum ({}), adjusting for visibility", 
				colorType, alpha, MIN_ALPHA);
			return new Color(configColor.getRed(), configColor.getGreen(), 
				configColor.getBlue(), MIN_ALPHA);
		}
		
		if (alpha > MAX_ALPHA)
		{
			log.warn("{} color alpha ({}) above maximum ({}), adjusting", 
				colorType, alpha, MAX_ALPHA);
			return new Color(configColor.getRed(), configColor.getGreen(), 
				configColor.getBlue(), MAX_ALPHA);
		}
		
		// Validate RGB values are within bounds
		int red = Math.max(0, Math.min(255, configColor.getRed()));
		int green = Math.max(0, Math.min(255, configColor.getGreen()));
		int blue = Math.max(0, Math.min(255, configColor.getBlue()));
		
		// Check if we had to adjust RGB values
		if (red != configColor.getRed() || green != configColor.getGreen() || blue != configColor.getBlue())
		{
			log.warn("{} color RGB values out of bounds, adjusted to ({}, {}, {})", 
				colorType, red, green, blue);
			return new Color(red, green, blue, alpha);
		}
		
		return configColor;
	}
	
	/**
	 * Applies consistent color scheme rules across all visual elements.
	 * This method ensures that colors work well together and maintain visual hierarchy.
	 * 
	 * @param baseColor The base color to apply consistency rules to
	 * @param colorRole The role of this color in the interface
	 * @return A color that follows consistent scheme rules
	 */
	private Color applyColorSchemeConsistency(Color baseColor, ColorRole colorRole)
	{
		switch (colorRole)
		{
			case OUTLINE:
				// Outline colors should be subtle but visible
				return ensureMinimumContrast(baseColor, 0.6f);
				
			case ACTIVE_DRAG:
				// Active drag should be prominent and attention-grabbing
				return ensureMinimumContrast(baseColor, 0.8f);
				
			case DROP_TARGET:
				// Drop targets should be clearly visible but not overwhelming
				return ensureMinimumContrast(baseColor, 0.7f);
				
			case INVALID_DROP:
				// Invalid areas should be clearly distinguishable and warning-like
				return ensureMinimumContrast(baseColor, 0.75f);
				
			case HOVER:
				// Hover effects should be subtle enhancements
				return ensureMinimumContrast(baseColor, 0.5f);
				
			default:
				return baseColor;
		}
	}
	
	/**
	 * Ensures a color has minimum contrast for visibility.
	 * Adjusts brightness if the color is too dark or too light.
	 */
	private Color ensureMinimumContrast(Color color, float minimumBrightness)
	{
		// Calculate perceived brightness using standard luminance formula
		float brightness = (0.299f * color.getRed() + 0.587f * color.getGreen() + 0.114f * color.getBlue()) / 255f;
		
		if (brightness < minimumBrightness)
		{
			// Brighten the color to meet minimum brightness
			float factor = minimumBrightness / Math.max(brightness, 0.1f);
			return brightenColor(color, factor);
		}
		
		return color;
	}
	
	/**
	 * Enum defining the different roles colors play in the drag interface.
	 * Used for applying consistent color scheme rules.
	 */
	private enum ColorRole
	{
		OUTLINE,      // Default draggable outline
		ACTIVE_DRAG,  // Currently dragged item
		DROP_TARGET,  // Valid drop position
		INVALID_DROP, // Invalid drop area
		HOVER         // Hover highlight
	}
}