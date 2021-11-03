package com.github.m0bilebtw;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.VarPlayer;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import javax.inject.Inject;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

@PluginDescriptor(
		name = "Skills Progress Bars",
		description = "Adds progress bars to the skills tab to show how close the next level ups are",
		tags = {"skills", "stats", "levels", "progress", "bars"}
)
@Slf4j
public class SkillsTabProgressBarsPlugin extends Plugin {

	private static final int SCRIPTID_STATS_INIT = 393;

	static final int MINIMUM_BAR_HEIGHT = 1;
	static final int MAXIMUM_BAR_HEIGHT = 32;

	private static final int INDENT_WIDTH_ONE_SIDE = 4; // The skill panel from OSRS indents 3 pixels at the bottom (and top)

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private SkillsTabProgressBarsConfig config;

	@Inject
	private SkillsTabProgressBarsPlugin plugin;

	@Inject
	private SkillsTabProgressBarsOverlay overlay;

	@Inject
	private OverlayManager overlayManager;

	private Widget currentWidget;
	private SkillBarWidgetGrouping[] skillBars = new SkillBarWidgetGrouping[SkillData.values().length];

	private float[] progressStartHSB;
	private float[] progressEndHSB;
	private float[] goalStartHSB;
	private float[] goalEndHSB;

	@Override
	protected void startUp() throws Exception {
		overlayManager.add(overlay);

		// If user already logged in, we must manually get the first xp state
		// Otherwise, it would only show bars for skills being trained, or need a world hop/relog to show all
		if (client.getGameState() == GameState.LOGGED_IN) {
			log.info("Plugin startup while logged in - manually finding skill progress and attaching hover listeners");
			calculateAndStoreProgressForAllSkillsToLevel();
			attachHoverListeners();
			clientThread.invoke(this::buildSkillBars);
		}
	}

	@Override
	protected void shutDown() throws Exception {
		overlayManager.remove(overlay);
		clientThread.invoke(this::removeSkillBars);
	}

	@Provides
	SkillsTabProgressBarsConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(SkillsTabProgressBarsConfig.class);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		if (SkillsTabProgressBarsConfig.GROUP.equals(event.getGroup())) {
			// Clear out the cached HSBAs when they change, so the getters can regenerate them.
			switch(event.getKey()) {
				case "progressBarStartColor":
					progressStartHSB = null;
					break;
				case "progressBarEndColor":
					progressEndHSB = null;
					break;
				case "goalBarStartColor":
					goalStartHSB = null;
					break;
				case "goalBarEndColor":
					goalEndHSB = null;
					break;
			}
			// Force an update to bar size and colours
			updateSkillBars();
		}
		if (event.getGroup().equalsIgnoreCase(SkillsTabProgressBarsConfig.GROUP)) {
			overlay.generateHSBAComponents();
		}
	}

	@Subscribe
	public void onScriptPreFired(ScriptPreFired event) {
		if (event.getScriptId() != SCRIPTID_STATS_INIT)	{
			return;
		}
		currentWidget = event.getScriptEvent().getSource();
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event) {
		if (event.getScriptId() != SCRIPTID_STATS_INIT || currentWidget == null) {
			return;
		}
		buildSkillBar(currentWidget);
	}

	/**
	 * If the plugin is started after the skill panel has been built, this will add the bar widgets that are needed.
	 */
	private void buildSkillBars()
	{
		Widget skillsContainer = client.getWidget(WidgetInfo.SKILLS_CONTAINER);
		if (skillsContainer == null) {
			return;
		}

		for (Widget skillTile : skillsContainer.getStaticChildren()) {
			buildSkillBar(skillTile);
		}
	}

	private void removeSkillBars()
	{
		for (SkillBarWidgetGrouping grouping : skillBars) {
			if (grouping == null) {
				continue;
			}
			Widget parent = grouping.getBarBackground().getParent();
			Widget[] children = parent.getChildren();
			for (int i = 0; i < children.length; i++) {
				Widget child = children[i];
				if (grouping.contains(child)) {
					children[i] = null;
				}
			}
		}
		skillBars = new SkillBarWidgetGrouping[SkillData.values().length];
	}

	/**
	 * Create the widgets needed for the bars to exist, and keep a reference to them
	 * Setting their position, size, and colour is done in {@link SkillsTabProgressBarsPlugin#updateSkillBar}
	 *
	 * @param parent The parent widget inside which the skill bar is created
	 */
	private void buildSkillBar(Widget parent) {
		int idx = WidgetInfo.TO_CHILD(parent.getId()) - 1;
		SkillData skill = SkillData.get(idx);
		if (skill == null) {
			return;
		}

		Widget barBackground = parent.createChild(-1, WidgetType.RECTANGLE);
		barBackground.setYPositionMode(WidgetPositionMode.ABSOLUTE_BOTTOM);
		barBackground.setWidthMode(WidgetSizeMode.MINUS);
		barBackground.setFilled(true);
		barBackground.setHasListener(true);

		Widget barForeground = parent.createChild(-1, WidgetType.RECTANGLE);
		barForeground.setYPositionMode(WidgetPositionMode.ABSOLUTE_BOTTOM);
		barForeground.setFilled(true);
		barForeground.setHasListener(true);

		Widget goalBackground = parent.createChild(-1, WidgetType.RECTANGLE);
		goalBackground.setYPositionMode(WidgetPositionMode.ABSOLUTE_BOTTOM);
		goalBackground.setWidthMode(WidgetSizeMode.MINUS);
		goalBackground.setFilled(true);
		goalBackground.setHasListener(true);

		Widget goalForeground = parent.createChild(-1, WidgetType.RECTANGLE);
		goalForeground.setYPositionMode(WidgetPositionMode.ABSOLUTE_BOTTOM);
		goalForeground.setFilled(true);
		goalForeground.setHasListener(true);

		JavaScriptCallback updateCallback = ev ->
			updateSkillBar(skill, barBackground, barForeground, goalBackground, goalForeground);

		barBackground.setOnVarTransmitListener(updateCallback);
		barForeground.setOnVarTransmitListener(updateCallback);
		goalBackground.setOnVarTransmitListener(updateCallback);
		goalForeground.setOnVarTransmitListener(updateCallback);

		updateSkillBar(skill, barBackground, barForeground, goalBackground, goalForeground);
		skillBars[idx] = new SkillBarWidgetGrouping(barBackground, barForeground, goalBackground, goalForeground);
	}

	/**
	 * Update all the skill bars that we're currently using, in the case that the config was changed.
	 */
	private void updateSkillBars() {
		clientThread.invoke(() -> {
			for (int i = 0; i < SkillData.values().length; i++)	{
				SkillData skill = SkillData.get(i);
				SkillBarWidgetGrouping widgets = skillBars[i];
				if (widgets != null) {
					updateSkillBar(skill,
						widgets.getBarBackground(), widgets.getBarForeground(),
						widgets.getGoalBackground(), widgets.getGoalForeground()
					);
				}
			}
		});
	}

	/**
	 * Update a specific skill's bar
	 * @param skill The skill to be updated
	 * @param barBackground The level progress bar background widget
	 * @param barForeground The level progress bar foreground widget
	 * @param goalBackground The goal progress bar background widget
	 * @param goalForeground The goal progress bar foreground widget
	 */
	private void updateSkillBar(SkillData skill,
								Widget barBackground, Widget barForeground,
								Widget goalBackground, Widget goalForeground) {
		final int currentXP = client.getSkillExperience(skill.getSkill());
		final int currentLevel = Experience.getLevelForXp(currentXP);
		final int currentLevelXP = Experience.getXpForLevel(currentLevel);
		final int nextLevelXP = currentLevel >= Experience.MAX_VIRT_LEVEL
			? Experience.MAX_SKILL_XP
			: Experience.getXpForLevel(currentLevel + 1);

		final int goalStartXP = client.getVar(skill.getGoalStartVarp());
		final int goalEndXP = client.getVar(skill.getGoalEndVarp());

		final double barPercent = Math.min(1.0, (currentXP - currentLevelXP) / (double)(nextLevelXP - currentLevelXP));
		final double goalPercent = Math.min(1.0, (currentXP - goalStartXP) / (double)(goalEndXP - goalStartXP));

		final int startX = config.indent() ? INDENT_WIDTH_ONE_SIDE : 0;
		int maxWidth = barForeground.getParent().getOriginalWidth();
		if (config.indent()) {
			maxWidth -= INDENT_WIDTH_ONE_SIDE * 2;
		}

		final boolean shouldRenderNormalBar = currentLevel < Experience.MAX_REAL_LEVEL || config.virtualLevels();
		final boolean shouldRenderGoalBar = goalEndXP > 0 && config.showGoals();

		int barHeight = config.barHeight();
		// If both bars are being drawn, drawn them at half height if their height would exceed the top of the widget
		if (barHeight > MAXIMUM_BAR_HEIGHT / 2 && shouldRenderNormalBar && shouldRenderGoalBar)	{
			barHeight /= 2;
		}

		if (shouldRenderNormalBar)	{
			barBackground.setHidden(false);
			barForeground.setHidden(false);

			barBackground.setOriginalX(startX);
			barBackground.setOriginalY(0);
			barBackground.setOriginalWidth(config.indent() ? INDENT_WIDTH_ONE_SIDE * 2 : 0);
			barBackground.setOriginalHeight(barHeight);
			barBackground.setTextColor(config.backgroundColor().getRGB());
			barBackground.setOpacity(255 - config.backgroundColor().getAlpha());

			final int progressWidth = (int) (maxWidth * barPercent);

			barForeground.setOriginalX(startX);
			barForeground.setOriginalY(0);
			barForeground.setOriginalWidth(progressWidth);
			barForeground.setOriginalHeight(barHeight);
			barForeground.setTextColor(lerpHSB(getProgressStartHSB(), getProgressEndHSB(), barPercent)); // interpolate between start and end
			barForeground.setOpacity(255 - lerpAlpha(config.progressBarStartColor(), config.progressBarEndColor(), barPercent));
		} else {
			barBackground.setHidden(true);
			barForeground.setHidden(true);
		}


		if (shouldRenderGoalBar) {
			final int yPos = barHeight * (shouldRenderNormalBar ? 1 : 0);

			goalBackground.setHidden(false);
			goalForeground.setHidden(false);

			goalBackground.setOriginalX(startX);
			goalBackground.setOriginalY(yPos);
			goalBackground.setOriginalWidth(config.indent() ? INDENT_WIDTH_ONE_SIDE * 2 : 0);
			goalBackground.setOriginalHeight(barHeight);
			goalBackground.setTextColor(config.backgroundColor().getRGB());
			goalBackground.setOpacity(255 - config.backgroundColor().getAlpha());

			final int goalWidth = (int) (maxWidth * goalPercent);

			goalForeground.setOriginalX(startX);
			goalForeground.setOriginalY(yPos);
			goalForeground.setOriginalWidth(goalWidth);
			goalForeground.setOriginalHeight(barHeight);
			goalForeground.setTextColor(lerpHSB(getGoalStartHSB(), getGoalEndHSB(), goalPercent)); // interpolate between start and end
			goalForeground.setOpacity(255 - lerpAlpha(config.goalBarStartColor(), config.goalBarEndColor(), goalPercent));
		} else {
			goalBackground.setHidden(true);
			goalForeground.setHidden(true);
		}
		barBackground.revalidate();
		barForeground.revalidate();
		goalBackground.revalidate();
		goalForeground.revalidate();
	}

	/**
	 * Linearly interpolate between two colours in HSB arrays
	 *
	 * @param start The starting colour, as a HSB array
	 * @param end The ending colour, as a HSB array
	 * @param percent Tow much of the ending colour to include as a percentage
	 * @return The integer representation of the interpolated colour's RGB value
	 */
	private int lerpHSB(float[] start, float[] end, double percent) {
		return Color.getHSBColor(
			(float) (start[0] + percent * (end[0] - start[0])),
			(float) (start[1] + percent * (end[1] - start[1])),
			(float) (start[2] + percent * (end[2] - start[2]))).getRGB();
	}


	/**
	 * Linearly interpolate between the alpha values of two colours
	 *
	 * @param start The starting colour
	 * @param end The ending colour
	 * @param percent how much of the ending colour to include as a percentage
	 * @return The interpolated alpha value
	 */
	private int lerpAlpha(Color start, Color end, double percent) {
		return (int) Math.round(start.getAlpha() + (percent * (end.getAlpha() - start.getAlpha())));
	}

	/**
	 * Convert the starting colour of the progress bar into a HSB array, with caching
	 */
	private float[] getProgressStartHSB() {
		if (progressStartHSB == null) {
			progressStartHSB = getHSBArray(config.progressBarStartColor());
		}
		return progressStartHSB;
	}

	/**
	 * Convert the starting colour of the progress bar into a HSB array, with caching
	 */
	private float[] getProgressEndHSB() {
		if (progressEndHSB == null) {
			progressEndHSB = getHSBArray(config.progressBarEndColor());
		}
		return progressEndHSB;
	}

	/**
	 * Convert the starting colour of the progress bar into a HSB array, with caching
	 */
	private float[] getGoalStartHSB() {
		if (goalStartHSB == null) {
			goalStartHSB = getHSBArray(config.goalBarStartColor());
		}
		return goalStartHSB;
	}

	/**
	 * Convert the starting colour of the progress bar into a HSB array, with caching
	 */
	private float[] getGoalEndHSB() {
		if (goalEndHSB == null) {
			goalEndHSB = getHSBArray(config.goalBarEndColor());
		}
		return goalEndHSB;
	}

	/**
	 * @param color The colour to convert
	 * @return The passed colour as a HSB array
	 */
	private float[] getHSBArray(Color color) {
		float[] arr = new float[3];
		Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), arr);
		return arr;
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

