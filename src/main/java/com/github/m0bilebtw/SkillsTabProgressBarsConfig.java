package com.github.m0bilebtw;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import net.runelite.client.config.Alpha;

import java.awt.Color;

@ConfigGroup(SkillsTabProgressBarsConfig.GROUP)
public interface SkillsTabProgressBarsConfig extends Config {

    String GROUP = "skillstabprogressbars";

    @Alpha
    @ConfigItem(
            keyName = "progressBarStartColor",
            name = "Start color",
            description = "The color from which the progress bar fades.",
            position = 0
    )
    default Color progressBarStartColor() {
        return new Color(0xFFFF0000);
    }

    @Alpha
    @ConfigItem(
            keyName = "progressBarEndColor",
            name = "End color",
            description = "The color to which the progress bar fades.",
            position = 1
    )
    default Color progressBarEndColor() {
        return new Color(0xFF00FF00);
    }

    @Alpha
    @ConfigItem(
            keyName = "backgroundColor",
            name = "Background color",
            description = "Sets the color for the background drawn behind progress bars.",
            position = 2
    )
    default Color backgroundColor() {
        return Color.BLACK;
    }

    @ConfigItem(
            keyName = "indent",
            name = "Match skill panel indent",
            description = "Progress bars and backgrounds will start and stop a few pixels indented to match the skill panel",
            position = 3
    )
    default boolean indent() {
        return true;
    }

    @Range(
            min = SkillsTabProgressBarsPlugin.MINIMUM_BAR_HEIGHT,
            max = SkillsTabProgressBarsPlugin.MAXIMUM_BAR_HEIGHT
    )
    @ConfigItem(
            keyName = "barHeight",
            name = "Progress bar height",
            description = "The total height of the progress bars displayed over the skills tab (with goals showing, height is shared between the bars if set too high)",
            position = 4
    )
    default int barHeight() {
        return 2;
    }

    @ConfigItem(
            keyName = "virtualLevels",
            name = "Show for virtual levels",
            description = "Show progress towards 'virtual levels' past 99",
            position = 5
    )
    default boolean virtualLevels() {
        return false;
    }

    @ConfigItem(
            keyName = "showGoals",
            name = "Show goals progress",
            description = "Show progress towards the goals you set using the in-game XP menu in addition to progress towards individual levels",
            position = 6
    )
    default boolean showGoals() {
        return false;
    }

    @ConfigItem(
            keyName = "showOnlyOnHover",
            name = "Show only when hovered",
            description = "Show progress only for the current skill hovered by your cursor",
            position = 7
    )
    default boolean showOnlyOnHover() {
        return false;
    }

    @Alpha
    @ConfigItem(
            keyName = "goalBarStartColor",
            name = "Goal start color",
            description = "The color from which the goal bar fades.",
            position = 8
    )
    default Color goalBarStartColor() {
        return new Color(0xFF0000FF);
    }

    @Alpha
    @ConfigItem(
            keyName = "goalBarEndColor",
            name = "Goal end color",
            description = "The color to which the goal bar fades.",
            position = 9
    )
    default Color goalBarEndColor() {
        return new Color(0xFFFF0080);
    }
}

