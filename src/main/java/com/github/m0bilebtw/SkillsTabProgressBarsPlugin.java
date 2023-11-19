package com.github.m0bilebtw;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.GameState;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import javax.inject.Inject;
import java.awt.Color;

@PluginDescriptor(
		name = "Skills Progress Bars",
		description = "Adds progress bars to the skills tab to show how close the next level ups are",
		tags = {"skills", "stats", "levels", "goals", "progress", "bars"}
)
@Slf4j
public class SkillsTabProgressBarsPlugin extends Plugin {

	private static final int SCRIPTID_STATS_INIT = 393;
	private static final int SCRIPTID_STATS_SKILLTOTAL = 396;

	static final int MINIMUM_BAR_HEIGHT = 1;
	static final int MAXIMUM_BAR_HEIGHT = 32;

	private static final int INDENT_WIDTH_ONE_SIDE = 4; // The skill panel from OSRS indents 3 pixels at the bottom (and top)

	private static final int WIDGET_CHILD_ID_MASK = 0xFFFF;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private SkillsTabProgressBarsConfig config;

	private Widget currentWidget;
	private SkillBarWidgetGrouping currentHovered;
	private SkillBarWidgetGrouping[] skillBars = new SkillBarWidgetGrouping[SkillData.values().length];

	private float[] progressStartHSB;
	private float[] progressEndHSB;
	private float[] goalStartHSB;
	private float[] goalEndHSB;

	@Override
	protected void startUp() {
		if (client.getGameState() == GameState.LOGGED_IN) {
			clientThread.invoke(this::buildSkillBars);
		}
	}

	@Override
	protected void shutDown() {
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
				case "showOnHover":
					handleContainerListener();
					break;
			}
			// Force an update to bar size and colours
			updateSkillBars();
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
		if (event.getScriptId() == SCRIPTID_STATS_INIT && currentWidget != null) {
			buildSkillBar(currentWidget);
		}
		// Add the container listener after all the other bars have been created
		// There's no specific reason to do it after, but this will always fire once on creation of the skills tab
		else if (event.getScriptId() == SCRIPTID_STATS_SKILLTOTAL) {
			handleContainerListener();
		}
	}

	/**
	 * If the plugin is started after the skill panel has been built, this will add the bar widgets that are needed.
	 */
	private void buildSkillBars() {
		Widget skillsContainer = client.getWidget(ComponentID.SKILLS_CONTAINER);
		if (skillsContainer == null) {
			return;
		}

		for (Widget skillTile : skillsContainer.getStaticChildren()) {
			buildSkillBar(skillTile);
		}
		handleContainerListener();
	}

	private void removeSkillBars() {
		for (SkillBarWidgetGrouping grouping : skillBars) {
			if (grouping == null) {
				continue;
			}
			Widget parent = grouping.getBarBackground().getParent();
			removeHoverListener(parent, grouping);
			Widget[] children = parent.getChildren();
			for (int i = 0; i < children.length; i++) {
				Widget child = children[i];
				if (grouping.contains(child)) {
					children[i] = null;
				}
			}
		}
		removeContainerListener();
		skillBars = new SkillBarWidgetGrouping[SkillData.values().length];
	}

	private int getChildId(int id) {
		return id & WIDGET_CHILD_ID_MASK;
	}

	/**
	 * Create the widgets needed for the bars to exist, and keep a reference to them
	 * Setting their position, size, and colour is done in {@link #updateSkillBar}
	 *
	 * @param parent The parent widget inside which the skill bar is created
	 */
	private void buildSkillBar(Widget parent) {
		int idx = getChildId(parent.getId()) - 1;
		SkillData skill = SkillData.get(idx);
		if (skill == null) {
			return;
		}

		Widget grayOut99 = parent.createChild(-1, WidgetType.RECTANGLE);
		grayOut99.setXPositionMode(WidgetPositionMode.ABSOLUTE_CENTER);
		grayOut99.setYPositionMode(WidgetPositionMode.ABSOLUTE_CENTER);
		grayOut99.setWidthMode(WidgetSizeMode.MINUS);
		grayOut99.setHeightMode(WidgetSizeMode.MINUS);
		grayOut99.setOriginalWidth(0);
		grayOut99.setOriginalHeight(0);
		grayOut99.setFilled(true);
		grayOut99.setHasListener(true);
		grayOut99.setTextColor(Color.BLACK.getRGB());

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

		SkillBarWidgetGrouping grouping = new SkillBarWidgetGrouping(grayOut99, barBackground, barForeground, goalBackground, goalForeground);

		JavaScriptCallback updateCallback = ev -> updateSkillBar(skill, grouping);

		grayOut99.setOnVarTransmitListener(updateCallback);
		barBackground.setOnVarTransmitListener(updateCallback);
		barForeground.setOnVarTransmitListener(updateCallback);
		goalBackground.setOnVarTransmitListener(updateCallback);
		goalForeground.setOnVarTransmitListener(updateCallback);

		updateSkillBar(skill, grouping);
		handleHoverListener(parent, grouping);
		skillBars[idx] = grouping;
	}

	/**
	 * Add or remove hover listeners on the provided widget.
	 *
	 * @param parent The widget containing the skill information and bars
	 * @param grouping The collection of widgets representing the bars
	 */
	private void handleHoverListener(Widget parent, SkillBarWidgetGrouping grouping) {
		if (config.showOnHover()) {
			addHoverListener(parent, grouping);
		}
		else {
			removeHoverListener(parent, grouping);
		}
	}

	/**
	 * See {@link #handleHoverListener}
	 */
	private void addHoverListener(Widget parent, SkillBarWidgetGrouping grouping) {
		Widget grayOut99 = grouping.getGrayOut99();
		Widget barBackground = grouping.getBarBackground();
		Widget barForeground = grouping.getBarForeground();
		Widget goalBackground = grouping.getGoalBackground();
		Widget goalForeground = grouping.getGoalForeground();

		grayOut99.setHidden(true);
		barBackground.setHidden(true);
		barForeground.setHidden(true);
		goalBackground.setHidden(true);
		goalForeground.setHidden(true);

		parent.setOnMouseOverListener((JavaScriptCallback) ev -> {
			// We need to hide the old hovered widgets so there aren't multiple visible
			// when moving the mouse between skills.
			if (currentHovered != null) {
				currentHovered.getGrayOut99().setHidden(true);
				currentHovered.getBarBackground().setHidden(true);
				currentHovered.getBarForeground().setHidden(true);
				currentHovered.getGoalBackground().setHidden(true);
				currentHovered.getGoalForeground().setHidden(true);
			}
			currentHovered = grouping;
			grayOut99.setHidden(false);
			barBackground.setHidden(false);
			barForeground.setHidden(false);
			goalBackground.setHidden(false);
			goalForeground.setHidden(false);
		});

		parent.setHasListener(true);
	}

	/**
	 * See {@link #handleHoverListener}
	 */
	private void removeHoverListener(Widget parent, SkillBarWidgetGrouping grouping) {
		Widget grayOut99 = grouping.getGrayOut99();
		Widget barBackground = grouping.getBarBackground();
		Widget barForeground = grouping.getBarForeground();
		Widget goalBackground = grouping.getGoalBackground();
		Widget goalForeground = grouping.getGoalForeground();

		grayOut99.setHidden(false);
		barBackground.setHidden(false);
		barForeground.setHidden(false);
		goalBackground.setHidden(false);
		goalForeground.setHidden(false);

		parent.setOnMouseOverListener((Object[]) null);
	}

	/**
	 * Add or remove a listener to hide the currently visible bar if needed.
	 * This needs to be added to the container as each of the skills use {@link Widget#setOnMouseLeaveListener}
	 * to handle the vanilla tooltip destruction.
	 */
	private void handleContainerListener() {
		if (config.showOnHover()) {
			addContainerListener();
		}
		else {
			removeContainerListener();
		}
	}

	/**
	 * See {@link #handleContainerListener}
	 */
	private void addContainerListener() {
		Widget container = client.getWidget(ComponentID.SKILLS_CONTAINER);
		if (container == null) {
			return;
		}

		container.setOnMouseLeaveListener((JavaScriptCallback) ev -> {
			if (currentHovered != null) {
				currentHovered.getGrayOut99().setHidden(true);
				currentHovered.getBarBackground().setHidden(true);
				currentHovered.getBarForeground().setHidden(true);
				currentHovered.getGoalBackground().setHidden(true);
				currentHovered.getGoalForeground().setHidden(true);
			}
			currentHovered = null;
		});
		container.setHasListener(true);
	}

	/**
	 * See {@link #handleContainerListener}
	 */
	private void removeContainerListener() {
		Widget container = client.getWidget(ComponentID.SKILLS_CONTAINER);
		if (container == null) {
			return;
		}
		container.setOnMouseLeaveListener((Object[]) null);
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
					updateSkillBar(skill, widgets);
					handleHoverListener(widgets.getBarBackground().getParent(), widgets);
				}
			}
		});
	}

	/**
	 * Update a specific skill's bar
	 * @param skill The skill to be updated
	 * @param grouping The collection of widgets to represent the progress and goal bars
	 */
	private void updateSkillBar(SkillData skill,
								SkillBarWidgetGrouping grouping) {
		Widget grayOut99 = grouping.getGrayOut99();
		Widget barBackground = grouping.getBarBackground();
		Widget barForeground = grouping.getBarForeground();
		Widget goalBackground = grouping.getGoalBackground();
		Widget goalForeground = grouping.getGoalForeground();

		final int currentXP = client.getSkillExperience(skill.getSkill());
		final int currentLevel = Experience.getLevelForXp(currentXP);
		final int currentLevelXP = Experience.getXpForLevel(currentLevel);
		final int nextLevelXP = currentLevel >= Experience.MAX_VIRT_LEVEL
			? Experience.MAX_SKILL_XP
			: Experience.getXpForLevel(currentLevel + 1);

		final int goalStartXP = client.getVarpValue(skill.getGoalStartVarp());
		final int goalEndXP = client.getVarpValue(skill.getGoalEndVarp());

		final double barPercent = Math.min(1.0, (currentXP - currentLevelXP) / (double)(nextLevelXP - currentLevelXP));
		final double goalPercent = Math.min(1.0, (currentXP - goalStartXP) / (double)(goalEndXP - goalStartXP));

		final int startX = config.indent() ? INDENT_WIDTH_ONE_SIDE : 0;
		int maxWidth = barForeground.getParent().getOriginalWidth();
		if (config.indent()) {
			maxWidth -= INDENT_WIDTH_ONE_SIDE * 2;
		}

		final boolean shouldGrayOut = (config.grayOut99() && currentLevel >= Experience.MAX_REAL_LEVEL) || (config.grayOut200m() && currentLevelXP >= Experience.MAX_SKILL_XP);
		final boolean shouldCalculateNormalBar = !config.showOnlyGoals() && (currentLevel < Experience.MAX_REAL_LEVEL || config.virtualLevels()) && currentLevelXP < Experience.MAX_SKILL_XP;
		final boolean shouldCalculateGoalBar = goalEndXP > 0 && config.showGoals();
		final boolean shouldRenderAnyBars = !config.showOnHover() || grouping == currentHovered;

		int barHeight = config.barHeight();
		// If both bars are being drawn, drawn them at half height if their height would exceed the top of the widget
		if (barHeight > MAXIMUM_BAR_HEIGHT / 2 && shouldCalculateNormalBar && shouldCalculateGoalBar)	{
			barHeight /= 2;
		}

		if (shouldGrayOut) {
			grayOut99.setOpacity(255 - config.grayOutOpacity());
		} else {
			// Set the gray out to be invisible so it doesn't conflict with the hover hiding
			grayOut99.setOpacity(255);
		}

		if (shouldCalculateNormalBar)	{
			barBackground.setHidden(!shouldRenderAnyBars);
			barForeground.setHidden(!shouldRenderAnyBars);

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
			// Set the bars to be invisible so they don't conflict with the hover hiding
			barBackground.setOpacity(255);
			barForeground.setOpacity(255);
		}


		if (shouldCalculateGoalBar) {
			final int yPos = barHeight * (shouldCalculateNormalBar ? 1 : 0);

			goalBackground.setHidden(!shouldRenderAnyBars);
			goalForeground.setHidden(!shouldRenderAnyBars);

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
			// Set the bars to be invisible so they don't conflict with the hover hiding
			goalBackground.setOpacity(255);
			goalForeground.setOpacity(255);
		}
		grayOut99.revalidate();
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
}

