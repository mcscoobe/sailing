package com.duckblade.osrs.sailing.features.trawling;

import com.duckblade.osrs.sailing.model.FishingAreaType;
import java.awt.Color;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Shoal
{

	// Shoal durations here are 2 ticks lower than wiki numbers to handle movement tracking
	GIANT_KRILL("Giant krill", new Color(0xd7774d), FishingAreaType.ONE_DEPTH, 0),
	HADDOCK("Haddock", new Color(0x7e919f), FishingAreaType.ONE_DEPTH, 0),
	YELLOWFIN("Yellowfin", new Color(0xebcd1c), FishingAreaType.TWO_DEPTH, 98),
	HALIBUT("Halibut", new Color(0xb08f54), FishingAreaType.TWO_DEPTH, 78),
	BLUEFIN("Bluefin", new Color(0x2a89a8), FishingAreaType.THREE_DEPTH, 68),
	MARLIN("Marlin", new Color(0xb9b7ad), FishingAreaType.THREE_DEPTH, 48);

	public static final Shoal[] VALUES = Shoal.values();

	private final String name;
	private final Color color;
	private final FishingAreaType depth;
	private final int stopDuration;

	@Override
	public String toString()
	{
		return name;
	}

	public static @Nullable Shoal byName(final String name)
	{
		for (final var shoal : VALUES)
		{
			if (shoal.name.equalsIgnoreCase(name))
			{
				return shoal;
			}
		}

		return null;
	}
}
