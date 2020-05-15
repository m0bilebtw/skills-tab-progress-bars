package com.github.m0bilebtw;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.VarPlayer;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
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
	static final int MAXIMUM_BAR_HEIGHT = 32;

	@Inject
	private Client client;

	@Inject
	private SkillsTabProgressBarsPlugin plugin;

	@Inject
	private SkillsTabProgressBarsOverlay overlay;

	@Inject
	private OverlayManager overlayManager;

	@Override
	protected void startUp() throws Exception {
		overlayManager.add(overlay);

		// If user already logged in, we must manually get the first xp state
		// Otherwise, it would only show bars for skills being trained, or need a world hop/relog to show all
		if (client.getGameState() == GameState.LOGGED_IN) {
			log.info("Plugin startup while logged in - manually finding skill progress and attaching hover listeners");
			calculateAndStoreProgressForAllSkillsToLevel();
			attachHoverListeners();
		}
	}

	@Override
	protected void shutDown() throws Exception {
		overlayManager.remove(overlay);
	}

	@Provides
	SkillsTabProgressBarsConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(SkillsTabProgressBarsConfig.class);
	}

	final Map<Skill, Double> progressToLevelNormalised = new HashMap<>();
	final Map<Skill, Double> progressToGoalNormalised = new HashMap<>();

	@Subscribe
	public void onStatChanged(StatChanged statChanged) {
		calculateAndStoreProgressToLevel(statChanged.getSkill(), statChanged.getXp());
		calculateAndStoreProgressToGoal(statChanged.getSkill(), statChanged.getXp());
	}

	private void calculateAndStoreProgressForAllSkillsToLevel() {
		for (Skill skill : Skill.values()) {
			if (skill == Skill.OVERALL) {
				// No calculation done for total level
				continue;
			}
			calculateAndStoreProgressToLevel(skill, client.getSkillExperience(skill));
			calculateAndStoreProgressToGoal(skill, client.getSkillExperience(skill));
		}
	}

	private void calculateAndStoreProgressToLevel(Skill skill, int currentXp) {
		double progressToLevelNormalised = 1d;
		int currentLevel = Experience.getLevelForXp(currentXp);
		if (currentLevel < Experience.MAX_VIRT_LEVEL) {
			final int xpForCurrentLevel = Experience.getXpForLevel(currentLevel);
			progressToLevelNormalised =
					(1d * (currentXp - xpForCurrentLevel)) /
							(Experience.getXpForLevel(currentLevel + 1) - xpForCurrentLevel);
		}
		this.progressToLevelNormalised.put(skill, progressToLevelNormalised);
	}

	private void calculateAndStoreProgressToGoal(Skill skill, int currentXp) {
		double progressToGoalNormalised = 1d;
		final VarPlayer startGoal = getStartGoalVarPlayer(skill);
		final VarPlayer endGoal = getEndGoalVarPlayer(skill);
		if (startGoal != null && endGoal != null) {
			final int startGoalXp = client.getVar(startGoal);
			final int endGoalXp = client.getVar(endGoal);
			progressToGoalNormalised =
					(1d * (currentXp - startGoalXp)) /
							(endGoalXp - startGoalXp);
		}
		this.progressToGoalNormalised.put(skill, progressToGoalNormalised);
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded widget) {
		if (widget.getGroupId() == WidgetInfo.SKILLS_CONTAINER.getGroupId()) {
			attachHoverListeners();
		}
	}

	static Skill hoveredSkill = null;

	private void attachHoverListeners() {
		Widget skillsContainer = client.getWidget(WidgetInfo.SKILLS_CONTAINER);
		if (skillsContainer == null) {
			log.info("skills container widget not found - not attaching hovered skill listeners");
			return;
		}

		for (Widget skillWidget : skillsContainer.getStaticChildren()) {
			final Skill skill = plugin.skillFromWidgetID(skillWidget.getId());
			if (skill != null) { /* skip invalid skill widgets (such as the side stone) */
				skillWidget.setOnMouseOverListener((JavaScriptCallback) event -> hoveredSkill = skill);
			}
		}
		skillsContainer.setOnMouseLeaveListener((JavaScriptCallback) event -> hoveredSkill = null);
	}

	Skill skillFromWidgetID(int widgetID) {
		// RuneLite provides no mapping for widget IDs -> Skill, so this is required and potentially liable to break upon updates
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
			case 20971544: return Skill.OVERALL;
			default: return null;
		}
	}

	private VarPlayer getStartGoalVarPlayer(Skill skill) {
		switch(skill) {
			case ATTACK: return VarPlayer.ATTACK_GOAL_START;
			case STRENGTH: return VarPlayer.STRENGTH_GOAL_START;
			case DEFENCE: return VarPlayer.DEFENCE_GOAL_START;
			case RANGED: return VarPlayer.RANGED_GOAL_START;
			case PRAYER: return VarPlayer.PRAYER_GOAL_START;
			case MAGIC: return VarPlayer.MAGIC_GOAL_START;
			case RUNECRAFT: return VarPlayer.RUNECRAFT_GOAL_START;
			case CONSTRUCTION: return VarPlayer.CONSTRUCTION_GOAL_START;
			case HITPOINTS: return VarPlayer.HITPOINTS_GOAL_START;
			case AGILITY: return VarPlayer.AGILITY_GOAL_START;
			case HERBLORE: return VarPlayer.HERBLORE_GOAL_START;
			case THIEVING: return VarPlayer.THIEVING_GOAL_START;
			case CRAFTING: return VarPlayer.CRAFTING_GOAL_START;
			case FLETCHING: return VarPlayer.FLETCHING_GOAL_START;
			case SLAYER: return VarPlayer.SLAYER_GOAL_START;
			case HUNTER: return VarPlayer.HUNTER_GOAL_START;
			case MINING: return VarPlayer.MINING_GOAL_START;
			case SMITHING: return VarPlayer.SMITHING_GOAL_START;
			case FISHING: return VarPlayer.FISHING_GOAL_START;
			case COOKING: return VarPlayer.COOKING_GOAL_START;
			case FIREMAKING: return VarPlayer.FIREMAKING_GOAL_START;
			case WOODCUTTING: return VarPlayer.WOODCUTTING_GOAL_START;
			case FARMING: return VarPlayer.FARMING_GOAL_START;
			default: return null;
		}
	}

	private VarPlayer getEndGoalVarPlayer(Skill skill) {
		switch(skill) {
			case ATTACK: return VarPlayer.ATTACK_GOAL_END;
			case STRENGTH: return VarPlayer.STRENGTH_GOAL_END;
			case DEFENCE: return VarPlayer.DEFENCE_GOAL_END;
			case RANGED: return VarPlayer.RANGED_GOAL_END;
			case PRAYER: return VarPlayer.PRAYER_GOAL_END;
			case MAGIC: return VarPlayer.MAGIC_GOAL_END;
			case RUNECRAFT: return VarPlayer.RUNECRAFT_GOAL_END;
			case CONSTRUCTION: return VarPlayer.CONSTRUCTION_GOAL_END;
			case HITPOINTS: return VarPlayer.HITPOINTS_GOAL_END;
			case AGILITY: return VarPlayer.AGILITY_GOAL_END;
			case HERBLORE: return VarPlayer.HERBLORE_GOAL_END;
			case THIEVING: return VarPlayer.THIEVING_GOAL_END;
			case CRAFTING: return VarPlayer.CRAFTING_GOAL_END;
			case FLETCHING: return VarPlayer.FLETCHING_GOAL_END;
			case SLAYER: return VarPlayer.SLAYER_GOAL_END;
			case HUNTER: return VarPlayer.HUNTER_GOAL_END;
			case MINING: return VarPlayer.MINING_GOAL_END;
			case SMITHING: return VarPlayer.SMITHING_GOAL_END;
			case FISHING: return VarPlayer.FISHING_GOAL_END;
			case COOKING: return VarPlayer.COOKING_GOAL_END;
			case FIREMAKING: return VarPlayer.FIREMAKING_GOAL_END;
			case WOODCUTTING: return VarPlayer.WOODCUTTING_GOAL_END;
			case FARMING: return VarPlayer.FARMING_GOAL_END;
			default: return null;
		}
	}
}

