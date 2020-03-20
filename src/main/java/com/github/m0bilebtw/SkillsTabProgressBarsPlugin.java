package com.github.m0bilebtw;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@PluginDescriptor(
		name = "Skills Progress Bars",
		description = "Adds progress bars to the skills tab to show how close the next level ups are",
		tags = {"skills", "stats", "levels", "progress", "bars"}
)

@Slf4j
public class SkillsTabProgressBarsPlugin extends Plugin {

	static final int MINIMUM_BAR_HEIGHT = 1;
	static final int MAXIMUM_BAR_HEIGHT = 15;
	static final int MINIMUM_BAR_WIDTH_TO_BE_SEEN_WELL = 2;

	@Inject
	private SkillsTabProgressBarsOverlay overlay;

	@Inject
	private OverlayManager overlayManager;

	@Override
	protected void startUp() throws Exception {
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown() throws Exception {
		overlayManager.remove(overlay);
	}

	@Provides
	SkillsTabProgressBarsConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(SkillsTabProgressBarsConfig.class);
	}

	final Map<Skill, Double> progressNormalised = new HashMap<>();

	@Subscribe
	public void onStatChanged(StatChanged statChanged) {
		final Skill skill = statChanged.getSkill();
		final int currentXp = statChanged.getXp();
		final int currentLevel = statChanged.getLevel();

		double progressToLevelNormalised = 1d;
		if (currentLevel < Experience.MAX_REAL_LEVEL) {
			final int xpForCurrentLevel = Experience.getXpForLevel(currentLevel);
			progressToLevelNormalised =
					(1d * (currentXp - xpForCurrentLevel)) /
							(Experience.getXpForLevel(currentLevel + 1) - xpForCurrentLevel);
		}
		progressNormalised.put(skill, progressToLevelNormalised);
	}

	Skill skillFromWidgetID(int widgetID) {
		// RuneLite provides no mapping for widget IDs -> Skill, so this is required */
		switch(widgetID) {
			case 20971521: return Skill.ATTACK;
			case 20971522: return Skill.STRENGTH;
			case 20971523: return Skill.DEFENCE;
			case 20971524: return Skill.RANGED;
			case 20971525: return Skill.PRAYER;
			case 20971526: return Skill.MAGIC;
			case 20971527: return Skill.RUNECRAFT;
			case 20971528: return Skill.CONSTRUCTION;
			case 20971529: return Skill.HITPOINTS;
			case 20971530: return Skill.AGILITY;
			case 20971531: return Skill.HERBLORE;
			case 20971532: return Skill.THIEVING;
			case 20971533: return Skill.CRAFTING;
			case 20971534: return Skill.FLETCHING;
			case 20971535: return Skill.SLAYER;
			case 20971536: return Skill.HUNTER;
			case 20971537: return Skill.MINING;
			case 20971538: return Skill.SMITHING;
			case 20971539: return Skill.FISHING;
			case 20971540: return Skill.COOKING;
			case 20971541: return Skill.FIREMAKING;
			case 20971542: return Skill.WOODCUTTING;
			case 20971543: return Skill.FARMING;
			default: return null;
		}
	}
}

