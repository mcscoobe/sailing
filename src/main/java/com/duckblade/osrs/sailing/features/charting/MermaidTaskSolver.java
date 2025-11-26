package com.duckblade.osrs.sailing.features.charting;

import com.duckblade.osrs.sailing.SailingConfig;
import com.duckblade.osrs.sailing.features.util.SailingUtil;
import com.duckblade.osrs.sailing.module.PluginLifecycleComponent;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

@Slf4j
@Singleton
public class MermaidTaskSolver
	extends OverlayPanel
	implements PluginLifecycleComponent
{

	@VisibleForTesting
	static final ImmutableSet<Integer> MERMAID_IDS = ImmutableSet.of(
		NpcID.SAILING_CHARTING_MERMAID_GUIDE_1,
		NpcID.SAILING_CHARTING_MERMAID_GUIDE_2,
		NpcID.SAILING_CHARTING_MERMAID_GUIDE_3,
		NpcID.SAILING_CHARTING_MERMAID_GUIDE_4,
		NpcID.SAILING_CHARTING_MERMAID_GUIDE_5
	);

	@VisibleForTesting
	static final Map<SeaChartTask, Map<String, Integer>> SOLUTIONS = ImmutableMap.<SeaChartTask, Map<String, Integer>>builder()
		.put(
			SeaChartTask.TASK_12,
			ImmutableMap.<String, Integer>builder()
				.put("Willow stock", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_44,
			ImmutableMap.<String, Integer>builder()
				.put("Pie dish", 1)
				.put("Pot of flour", 1)
				.put("Cooking apple", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_45,
			ImmutableMap.<String, Integer>builder()
				.put("Iron med helm", 1)
				.put("Bronze chainbody", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_46,
			ImmutableMap.<String, Integer>builder()
				.put("Cabbage seeds", 5)
				.build()
		)
		.put(
			SeaChartTask.TASK_47,
			ImmutableMap.<String, Integer>builder()
				.put("Watermelon", 10)
				.build()
		)
		.put(
			SeaChartTask.TASK_48,
			ImmutableMap.<String, Integer>builder()
				.put("Vial", 1)
				.put("Avantoe", 1)
				.put("Snape grass", 1)
				.put("Caviar", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_49,
			ImmutableMap.<String, Integer>builder()
				.put("Harralander potion (unf)", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_50,
			ImmutableMap.<String, Integer>builder()
				.put("Papaya fruit", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_51,
			ImmutableMap.<String, Integer>builder()
				.put("Ashes", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_52,
			ImmutableMap.<String, Integer>builder()
				.put("Bucket of sap", 1)
				.put("Raw slimy eel", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_53,
			ImmutableMap.<String, Integer>builder()
				.put("Barley", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_125,
			ImmutableMap.<String, Integer>builder()
				.put("Earth impling jar", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_127,
			ImmutableMap.<String, Integer>builder()
				.put("Cabbage", 1)
				.put("Onion", 1)
				.put("Tomato", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_126,
			ImmutableMap.<String, Integer>builder()
				.put("Coal", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_128,
			ImmutableMap.<String, Integer>builder()
				.put("Kwuarm", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_129,
			ImmutableMap.<String, Integer>builder()
				.put("Dwellberries", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_152,
			ImmutableMap.<String, Integer>builder()
				.put("Black flowers", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_153,
			ImmutableMap.<String, Integer>builder()
				.put("Butterfly jar", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_154,
			ImmutableMap.<String, Integer>builder()
				.put("Calquat keg", 2)
				.put("Ale yeast", 1)
				.put("Oak roots", 1)
				.put("Barley malt", 2)
				.build()
		)
		.put(
			SeaChartTask.TASK_155,
			ImmutableMap.<String, Integer>builder()
				.put("Vial", 1)
				.put("Coconut", 1)
				.put("Toadflax", 1)
				.put("Yew roots", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_156,
			ImmutableMap.<String, Integer>builder()
				.put("Soiled page", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_170,
			ImmutableMap.<String, Integer>builder()
				.put("Thatch spar dense", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_171,
			ImmutableMap.<String, Integer>builder()
				.put("Gold ore", 2)
				.build()
		)
		.put(
			SeaChartTask.TASK_172,
			ImmutableMap.<String, Integer>builder()
				.put("Malicious ashes", 2)
				.build()
		)
		.put(
			SeaChartTask.TASK_192,
			ImmutableMap.<String, Integer>builder()
				.put("Sandwich lady bottom", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_241,
			ImmutableMap.<String, Integer>builder()
				.put("Kharyrll teleport", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_242,
			ImmutableMap.<String, Integer>builder()
				.put("Raw cod", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_243,
			ImmutableMap.<String, Integer>builder()
				.put("Bronze limbs", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_244,
			ImmutableMap.<String, Integer>builder()
				.put("Onion", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_245,
			ImmutableMap.<String, Integer>builder()
				.put("Torstol", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_246,
			ImmutableMap.<String, Integer>builder()
				.put("Needle", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_247,
			ImmutableMap.<String, Integer>builder()
				.put("Clockwork", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_248,
			ImmutableMap.<String, Integer>builder()
				.put("Shield left half", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_249,
			ImmutableMap.<String, Integer>builder()
				.put("Vial of blood", 1)
				.put("Cadantine", 1)
				.put("Wine of zamorak", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_250,
			ImmutableMap.<String, Integer>builder()
				.put("Dragon bitter", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_251,
			ImmutableMap.<String, Integer>builder()
				.put("Rain bow", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_252,
			ImmutableMap.<String, Integer>builder()
				.put("Royal crown", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_271,
			ImmutableMap.<String, Integer>builder()
				.put("Nose peg", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_272,
			ImmutableMap.<String, Integer>builder()
				.put("Charcoal", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_273,
			ImmutableMap.<String, Integer>builder()
				.put("Woad leaf", 2)
				.put("Onion", 2)
				.build()
		)
		.put(
			SeaChartTask.TASK_274,
			ImmutableMap.<String, Integer>builder()
				.put("Swamp weed", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_307,
			ImmutableMap.<String, Integer>builder()
				.put("Stripy feather", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_308,
			ImmutableMap.<String, Integer>builder()
				.put("Equa leaves", 1)
				.put("Batta tin", 1)
				.put("Tomato", 2)
				.put("Cheese", 1)
				.put("Dwellberries", 1)
				.put("Onion", 1)
				.put("Cabbage", 1)
				.put("Gianne dough", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_309,
			ImmutableMap.<String, Integer>builder()
				.put("Lime", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_310,
			ImmutableMap.<String, Integer>builder()
				.put("Fedora", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_311,
			ImmutableMap.<String, Integer>builder()
				.put("Common tench", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_312,
			ImmutableMap.<String, Integer>builder()
				.put("Plank", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_313,
			ImmutableMap.<String, Integer>builder()
				.put("Ghrazi rapier", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_321,
			ImmutableMap.<String, Integer>builder()
				.put("Silver ore", 1)
				.put("Chisel", 1)
				.put("Uncut jade", 1)
				.put("Ring mould", 1)
				.put("Cosmic rune", 1)
				.put("Air rune", 3)
				.build()
		)
		.put(
			SeaChartTask.TASK_322,
			ImmutableMap.<String, Integer>builder()
				.put("Potato", 1)
				.put("Potato cactus", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_323,
			ImmutableMap.<String, Integer>builder()
				.put("Sandstone (10kg)", 1)
				.put("Sandstone (2kg)", 1)
				.put("Sandstone (1kg)", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_324,
			ImmutableMap.<String, Integer>builder()
				.put("Dark bow tie", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_325,
			ImmutableMap.<String, Integer>builder()
				.put("Double eye patch", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_326,
			ImmutableMap.<String, Integer>builder()
				.put("Bucket helm (g)", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_348,
			ImmutableMap.<String, Integer>builder()
				.put("Ring mould", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_349,
			ImmutableMap.<String, Integer>builder()
				.put("Bob's blue shirt", 1)
				.put("Bob's purple shirt", 1)
				.build()
		)
		.put(
			SeaChartTask.TASK_350,
			ImmutableMap.<String, Integer>builder()
				.put("Tarromin", 1)
				.build()
		)
		.build();

	private final Client client;
	private final SeaChartTaskIndex taskIndex;

	private SeaChartTask task;
	private Map<String, Integer> solution;

	@Inject
	public MermaidTaskSolver(Client client, SeaChartTaskIndex taskIndex)
	{
		this.client = client;
		this.taskIndex = taskIndex;

		setPreferredPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
	}

	@Override
	public boolean isEnabled(SailingConfig config)
	{
		return config.chartingMermaidSolver();
	}

	@Override
	public void shutDown()
	{
		reset();
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		if (task == null)
		{
			return;
		}

		if (task.isComplete(client))
		{
			log.debug("task {} completed, clearing", task.getTaskId());
			reset();
			return;
		}

		if (!SailingUtil.isSailing(client) || task.getLocation().distanceTo(SailingUtil.getTopLevelWorldPoint(client)) > 25)
		{
			log.debug("cancelling in progress task {} due to distance", task.getTaskId());
			reset();
		}
	}

	@Subscribe
	public void onInteractingChanged(InteractingChanged e)
	{
		if (!SailingUtil.isLocalPlayer(client, e.getSource()) || e.getTarget() == null)
		{
			return;
		}

		reset();

		Actor target = e.getTarget();
		if (!(target instanceof NPC) || !MERMAID_IDS.contains(((NPC) target).getId()))
		{
			return;
		}

		WorldPoint playerLoc = SailingUtil.getTopLevelWorldPoint(client);
		SeaChartTask maybeTask = taskIndex.findTask((NPC) target);
		if (maybeTask == null)
		{
			log.warn("no mermaid task found at {}", playerLoc);
			return;
		}
		task = maybeTask;

		solution = SOLUTIONS.get(maybeTask);
		if (solution == null)
		{
			log.warn("no solution found for task {}", task.getTaskId());
			reset();
			return;
		}

		log.debug("solution for task {} is {}", task.getTaskId(), solution);
	}

	private void reset()
	{
		task = null;
		solution = null;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (solution == null || !SailingUtil.isSailing(client))
		{
			return null;
		}

		List<LayoutableRenderableEntity> children = getPanelComponent().getChildren();
		children.add(TitleComponent.builder()
			.text("Mermaid Puzzle Solution")
			.build());

		for (Map.Entry<String, Integer> e : solution.entrySet())
		{
			children.add(LineComponent.builder()
				.left(e.getKey())
				.right(String.valueOf(e.getValue()))
				.build());
		}

		return super.render(graphics);
	}
}
