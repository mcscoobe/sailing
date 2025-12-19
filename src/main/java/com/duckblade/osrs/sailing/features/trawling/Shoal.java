package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.model.FishingAreaType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Shoal
{

	// Shoal durations here are 2 ticks lower than wiki numbers to handle movement tracking
	GIANT_KRILL(FishingAreaType.ONE_DEPTH, 0),
	HADDOCK(FishingAreaType.ONE_DEPTH, 0),
	YELLOWFIN(FishingAreaType.TWO_DEPTH, 98),
	HALIBUT(FishingAreaType.TWO_DEPTH, 78),
	BLUEFIN(FishingAreaType.THREE_DEPTH, 68),
	MARLIN(FishingAreaType.THREE_DEPTH, 48);

	private final FishingAreaType depth;
	private final int stopDuration;
}
