package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;

@Slf4j
@Singleton
public class HardcodedShoalPathOverlay extends Overlay implements PluginLifecycleComponent {

	@Nonnull
	private final Client client;
	private final SailingConfig config;
	
	// Port Roberts area boundaries (top-level coordinates)
	private static final int PORT_ROBERTS_WEST = 1822;
	private static final int PORT_ROBERTS_EAST = 2050;
	private static final int PORT_ROBERTS_SOUTH = 3129;
	private static final int PORT_ROBERTS_NORTH = 3414;
	
	// Southern Expanse area boundaries (top-level coordinates)
	private static final int SOUTHERN_EXPANSE_WEST = 1870;
	private static final int SOUTHERN_EXPANSE_EAST = 2180;
	private static final int SOUTHERN_EXPANSE_SOUTH = 2171;
	private static final int SOUTHERN_EXPANSE_NORTH = 2512;
	
	// Rainbow Reef area boundaries (top-level coordinates)
	private static final int RAINBOW_REEF_WEST = 2075;
	private static final int RAINBOW_REEF_EAST = 2406;
	private static final int RAINBOW_REEF_SOUTH = 2179;
	private static final int RAINBOW_REEF_NORTH = 2450;
	
	// Stop point indices for HALIBUT_PORT_ROBERTS (9 stop points)
	private static final int[] PORT_ROBERTS_STOP_INDICES = {0, 45, 79, 139, 168, 214, 258, 306, 337};
	
	// Stop point indices for HALIBUT_SOUTHERN_EXPANSE (10 stop points)
	private static final int[] SOUTHERN_EXPANSE_STOP_INDICES = {0, 15, 60, 97, 132, 185, 273, 343, 369, 419};
	
	// Stop point indices for BLUEFIN_RAINBOW_REEF (10 stop points)
	private static final int[] RAINBOW_REEF_STOP_INDICES = {0, 20, 52, 73, 108, 155, 188, 221, 264, 313};
	
	// Color for stop point overlays (red)
	private static final Color STOP_POINT_COLOR = Color.RED;

	@Inject
	public HardcodedShoalPathOverlay(@Nonnull Client client, SailingConfig config) {
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(PRIORITY_LOW);
	}

	@Override
	public boolean isEnabled(SailingConfig config) {
		return config.trawlingShowHardcodedShoalPaths();
	}

	@Override
	public void startUp() {
		log.debug("HardcodedShoalPathOverlay started");
	}

	@Override
	public void shutDown() {
		log.debug("HardcodedShoalPathOverlay shut down");
	}

	@Override
	public Dimension render(Graphics2D graphics) {
		// Only render paths if player is sailing
		if (!SailingUtil.isSailing(client)) {
			return null;
		}
		
		// Get top-level world coordinates (actual world position, not boat instance position)
		WorldPoint playerLocation = SailingUtil.getTopLevelWorldPoint(client);
		if (playerLocation == null) {
			return null;
		}

		Color pathColor = config.trawlingHardcodedShoalPathColour();
		
		// Only render Port Roberts path if player is within the Port Roberts area
		if (isInArea(playerLocation, PORT_ROBERTS_WEST, PORT_ROBERTS_EAST, PORT_ROBERTS_SOUTH, PORT_ROBERTS_NORTH)) {
			renderPath(graphics, ShoalPaths.HALIBUT_PORT_ROBERTS, pathColor, "Halibut - Port Roberts");
			renderStopPoints(graphics, ShoalPaths.HALIBUT_PORT_ROBERTS, PORT_ROBERTS_STOP_INDICES);
		}
		
		// Only render Southern Expanse path if player is within the Southern Expanse area
		if (isInArea(playerLocation, SOUTHERN_EXPANSE_WEST, SOUTHERN_EXPANSE_EAST, SOUTHERN_EXPANSE_SOUTH, SOUTHERN_EXPANSE_NORTH)) {
			renderPath(graphics, ShoalPaths.HALIBUT_SOUTHERN_EXPANSE, pathColor, "Halibut - Southern Expanse");
			renderStopPoints(graphics, ShoalPaths.HALIBUT_SOUTHERN_EXPANSE, SOUTHERN_EXPANSE_STOP_INDICES);
		}
		
		// Only render Rainbow Reef path if player is within the Rainbow Reef area
		if (isInArea(playerLocation, RAINBOW_REEF_WEST, RAINBOW_REEF_EAST, RAINBOW_REEF_SOUTH, RAINBOW_REEF_NORTH)) {
			renderPath(graphics, ShoalPaths.BLUEFIN_RAINBOW_REEF, pathColor, "Bluefin - Rainbow Reef");
			renderStopPoints(graphics, ShoalPaths.BLUEFIN_RAINBOW_REEF, RAINBOW_REEF_STOP_INDICES);
		}
		
		return null;
	}

	/**
	 * Check if the player is within a specific rectangular area.
	 * @param playerLocation The player's current world location
	 * @param westX Western boundary (minimum X)
	 * @param eastX Eastern boundary (maximum X)
	 * @param southY Southern boundary (minimum Y)
	 * @param northY Northern boundary (maximum Y)
	 * @return true if player is within the bounds
	 */
	private boolean isInArea(WorldPoint playerLocation, int westX, int eastX, int southY, int northY) {
		int x = playerLocation.getX();
		int y = playerLocation.getY();
		return x >= westX && x <= eastX && y >= southY && y <= northY;
	}

	private void renderPath(Graphics2D graphics, WorldPoint[] path, Color pathColor, String label) {
		if (path == null || path.length < 2) {
			return;
		}

		graphics.setStroke(new BasicStroke(2));
		net.runelite.api.Point previousCanvasPoint = null;
		net.runelite.api.Point firstVisiblePoint = null;

		for (WorldPoint worldPos : path) {
			// Convert WorldPoint to LocalPoint for rendering
			LocalPoint localPos = LocalPoint.fromWorld(client, worldPos);
			if (localPos == null) {
				previousCanvasPoint = null;
				continue;
			}

			net.runelite.api.Point canvasPoint = Perspective.localToCanvas(client, localPos, worldPos.getPlane());

			if (canvasPoint == null) {
				previousCanvasPoint = null;
				continue;
			}

			// Track first visible point for label
			if (firstVisiblePoint == null) {
				firstVisiblePoint = canvasPoint;
			}

			// Draw line from previous point
			if (previousCanvasPoint != null) {
				graphics.setColor(pathColor);
				graphics.drawLine(
					previousCanvasPoint.getX(),
					previousCanvasPoint.getY(),
					canvasPoint.getX(),
					canvasPoint.getY()
				);
			}

			previousCanvasPoint = canvasPoint;
		}

		// Draw line back to start to complete the loop
		if (path.length >= 2) {
			WorldPoint firstWorldPos = path[0];
			WorldPoint lastWorldPos = path[path.length - 1];

			LocalPoint firstLocal = LocalPoint.fromWorld(client, firstWorldPos);
			LocalPoint lastLocal = LocalPoint.fromWorld(client, lastWorldPos);

			if (firstLocal != null && lastLocal != null) {
				net.runelite.api.Point firstCanvas = Perspective.localToCanvas(client, firstLocal, firstWorldPos.getPlane());
				net.runelite.api.Point lastCanvas = Perspective.localToCanvas(client, lastLocal, lastWorldPos.getPlane());

				if (firstCanvas != null && lastCanvas != null) {
					// Draw dashed line to indicate loop
					Stroke dashed = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
						0, new float[]{9}, 0);
					graphics.setStroke(dashed);
					graphics.setColor(pathColor);
					graphics.drawLine(
						lastCanvas.getX(),
						lastCanvas.getY(),
						firstCanvas.getX(),
						firstCanvas.getY()
					);
				}
			}
		}


	}

	private void renderStopPoints(Graphics2D graphics, WorldPoint[] path, int[] stopIndices) {
		if (path == null || stopIndices == null) {
			return;
		}

		for (int index : stopIndices) {
			if (index >= path.length) {
				continue;
			}

			WorldPoint stopPoint = path[index];
			renderStopPointArea(graphics, stopPoint);
		}
	}

	private void renderStopPointArea(Graphics2D graphics, WorldPoint centerPoint) {
		// Convert WorldPoint to LocalPoint
		LocalPoint localPoint = LocalPoint.fromWorld(client, centerPoint);
		if (localPoint == null) {
			return;
		}

		// Convert to canvas point
		net.runelite.api.Point canvasPoint = Perspective.localToCanvas(client, localPoint, centerPoint.getPlane());
		if (canvasPoint == null) {
			return;
		}

		// Draw stop point marker - red filled circle with white outline (matches trace rendering)
		graphics.setColor(STOP_POINT_COLOR);
		graphics.fillOval(canvasPoint.getX() - 5, canvasPoint.getY() - 5, 10, 10);
		graphics.setColor(Color.WHITE);
		graphics.drawOval(canvasPoint.getX() - 5, canvasPoint.getY() - 5, 10, 10);
	}
}
