package com.github.m0bilebtw;

import net.runelite.client.config.*;

import java.awt.Color;

@ConfigGroup(SkillsTabProgressBarsConfig.GROUP)
public interface SkillsTabProgressBarsConfig extends Config {

    String GROUP = "skillstabprogressbars";

    @ConfigSection(
            name = "Bar Colours",
            description = "Settings for level and goal progress bar colours.",
            position = 20
    )
    String sectionColours = "Bar Colours";


    @ConfigItem(
            keyName = "indent",
            name = "Match skill panel indent",
            description = "Progress bars and backgrounds will start and stop a few pixels indented to match the skill panel.",
            position = 1
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
            description = "The total height of the progress bars displayed over the skills tab (with goals showing, height is shared between the bars if set too high).",
            position = 2
    )
    default int barHeight() {
        return 2;
    }

    @ConfigItem(
            keyName = "virtualLevels",
            name = "Show for virtual levels",
            description = "Show progress towards 'virtual levels' past 99.",
            position = 3
    )
    default boolean virtualLevels() {
        return false;
    }

    @ConfigItem(
            keyName = "grayOut99",
            name = "Darken skills at 99",
            description = "Show levels at 99 as darker than the others.",
            position = 4
    )
    default boolean grayOut99() {
        return false;
    }

    @ConfigItem(
            keyName = "showGoals",
            name = "Show goals progress",
            description = "Show progress towards the goals you set using the in-game XP menu in addition to progress towards individual levels.",
            position = 5
    )
    default boolean showGoals() {
        return false;
    }

    @ConfigItem(
            keyName = "showOnlyGoals",
            name = "Show ONLY goals progress",
            description = "Hides progress towards individual levels leaving only goals progress.",
            position = 6
    )
    default boolean showOnlyGoals() {
        return false;
    }

    @ConfigItem(
            keyName = "showOnlyOnHover",
            name = "Only show when hovered",
            description = "Only show the progress bars and goals for a skill when it is being hovered over.",
            position = 7
    )
    default boolean showOnHover() {
        return false;
    }

    @Alpha
    @ConfigItem(
            keyName = "progressBarStartColor",
            name = "Start color",
            description = "The color from which the progress bar fades.",
            section = sectionColours,
            position = 1
    )
    default Color progressBarStartColor() {
        return new Color(0xFFFF0000);
    }

    @Alpha
    @ConfigItem(
            keyName = "progressBarEndColor",
            name = "End color",
            description = "The color to which the progress bar fades.",
            section = sectionColours,
            position = 2
    )
    default Color progressBarEndColor() {
        return new Color(0xFF00FF00);
    }

    @Alpha
    @ConfigItem(
            keyName = "backgroundColor",
            name = "Background color",
            description = "Sets the color for the background drawn behind progress bars.",
            section = sectionColours,
            position = 3
    )
    default Color backgroundColor() {
        return Color.BLACK;
    }

    @Alpha
    @ConfigItem(
            keyName = "goalBarStartColor",
            name = "Goal start color",
            description = "The color from which the goal bar fades.",
            section = sectionColours,
            position = 4
    )
    default Color goalBarStartColor() {
        return new Color(0xFF0000FF);
    }

    @Alpha
    @ConfigItem(
            keyName = "goalBarEndColor",
            name = "Goal end color",
            description = "The color to which the goal bar fades.",
            section = sectionColours,
            position = 5
    )
    default Color goalBarEndColor() {
        return new Color(0xFFFF0080);
    }

    @Range(
            min = 0,
            max = 255
    )
    @ConfigItem(
            keyName = "grayOutOpacity",
            name = "Darken skills opacity",
            description = "Controls how dark skills are when level 99, and 'Darken skills at 99' is turned on.",
            section = sectionColours,
            position = 6
    )
    default int grayOutOpacity() {
        return 127;
    }
}

