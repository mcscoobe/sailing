package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.WorldEntity;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.WorldEntitySpawned;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Set;

/**
 * Service component that tracks the current depth state of active shoals.
 * Provides centralized depth tracking for use by overlays and other components.
 */
@Slf4j
@Singleton
public class ShoalDepthTracker implements PluginLifecycleComponent
{
	// WorldEntity config ID for moving shoals
	private static final int SHOAL_WORLD_ENTITY_CONFIG_ID = 4;

	// Shoal object IDs - used to detect any shoal presence
	private static final Set<Integer> SHOAL_OBJECT_IDS = ImmutableSet.of(
		TrawlingData.ShoalObjectID.MARLIN,
		TrawlingData.ShoalObjectID.BLUEFIN,
		TrawlingData.ShoalObjectID.VIBRANT,
		TrawlingData.ShoalObjectID.HALIBUT,
		TrawlingData.ShoalObjectID.GLISTENING,
		TrawlingData.ShoalObjectID.YELLOWFIN,
		TrawlingData.ShoalObjectID.HADDOCK,
		TrawlingData.ShoalObjectID.GIANT_KRILL,
		TrawlingData.ShoalObjectID.SHIMMERING
	);

	private final Client client;

	/**
	 * The current depth of the active shoal, or null if no shoal is active.
	 */
	@Getter
	private NetDepth currentDepth;

	/**
	 * Whether the current fishing area supports all three depth levels.
	 * True for Bluefin and Marlin areas, false for other areas.
	 */
	@Getter
	private boolean threeDepthArea;

	/**
	 * The direction of the next depth transition when at moderate depth in three-depth areas.
	 * Used to determine button highlighting when shoal is at moderate depth.
	 */
	@Getter
	private MovementDirection nextMovementDirection;

	/**
	 * The world location of the currently active shoal, or null if no shoal is active.
	 */
	private WorldPoint activeShoalLocation;

	/**
	 * The fishing area of the currently active shoal, or null if no shoal is active.
	 */
	private ShoalFishingArea currentFishingArea;

	@Inject
	public ShoalDepthTracker(Client client)
	{
		this.client = client;
	}

	/**
	 * Service components are always enabled.
	 *
	 * @param config the plugin configuration
	 * @return true
	 */
	@Override
	public boolean isEnabled(SailingConfig config)
	{
		return true;
	}

	/**
	 * Initialize the tracker state on startup.
	 */
	@Override
	public void startUp()
	{
		log.debug("ShoalDepthTracker started");
		clearState();
	}

	/**
	 * Clean up the tracker state on shutdown.
	 */
	@Override
	public void shutDown()
	{
		log.debug("ShoalDepthTracker shut down");
		clearState();
	}

	/**
	 * Clear all tracking state.
	 */
	private void clearState()
	{
		currentDepth = null;
		threeDepthArea = false;
		nextMovementDirection = MovementDirection.UNKNOWN;
		activeShoalLocation = null;
		currentFishingArea = null;
	}

	/**
	 * Handle shoal GameObject spawning.
	 * This is called when a shoal appears in the game world.
	 */
	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		GameObject obj = event.getGameObject();
		int objectId = obj.getId();

		if (SHOAL_OBJECT_IDS.contains(objectId))
		{
			log.debug("Shoal GameObject detected (ID={}), waiting for WorldEntity to get proper coordinates", objectId);
			// Don't initialize state yet - wait for WorldEntity spawn to get proper top-level coordinates
		}
	}

	/**
	 * Handle shoal WorldEntity spawning.
	 * This provides the accurate world location for the shoal.
	 */
	@Subscribe
	public void onWorldEntitySpawned(WorldEntitySpawned event)
	{
		WorldEntity entity = event.getWorldEntity();

		// Only track shoal WorldEntity
		if (entity.getConfig() != null && entity.getConfig().getId() == SHOAL_WORLD_ENTITY_CONFIG_ID)
		{
			LocalPoint localPos = entity.getCameraFocus();
			if (localPos != null)
			{
				WorldPoint worldPos = WorldPoint.fromLocal(client, localPos);
				if (worldPos != null)
				{
					initializeShoalState(worldPos);
				}
			}
		}
	}

	/**
	 * Initialize shoal tracking state based on the spawn location.
	 *
	 * @param location the world location where the shoal spawned
	 */
	private void initializeShoalState(WorldPoint location)
	{
		// Determine fishing area from location
		ShoalFishingArea fishingArea = TrawlingData.FishingAreas.getFishingAreaForLocation(location);

		if (fishingArea == null)
		{
			log.warn("Shoal spawned at unknown location: {} (not in any defined fishing area)", location);
			clearState();
			return;
		}

		// Initialize state based on fishing area
		activeShoalLocation = location;
		currentFishingArea = fishingArea;
		currentDepth = fishingArea.getStartingDepth();
		threeDepthArea = fishingArea.isThreeDepthArea();
		nextMovementDirection = MovementDirection.UNKNOWN;

		log.info("Shoal spawned at {} in {} area (starting depth: {}, three-depth: {})",
			location, fishingArea.isThreeDepthArea() ? "three-depth" : "two-depth",
			currentDepth, threeDepthArea);
	}

	/**
	 * Handle shoal GameObject despawning.
	 * This is called when a shoal leaves the game world.
	 */
	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		GameObject obj = event.getGameObject();
		int objectId = obj.getId();

		if (SHOAL_OBJECT_IDS.contains(objectId))
		{
			log.debug("Shoal despawned (left world view): ID={}", objectId);
			clearState();
		}
	}
}
