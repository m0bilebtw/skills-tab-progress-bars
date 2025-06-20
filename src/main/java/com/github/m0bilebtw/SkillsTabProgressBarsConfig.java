package com.github.m0bilebtw;

import net.runelite.api.Experience;
import net.runelite.client.config.*;

import java.awt.Color;

@ConfigGroup(SkillsTabProgressBarsConfig.GROUP)
public interface SkillsTabProgressBarsConfig extends Config {

    String GROUP = "skillstabprogressbars";

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
            keyName = "stillShowAt200m",
            name = "Still show at 200m",
            description = "Show full progress bar at 200m XP. This was previous unintentional behaviour, kept available for those that want it.",
            position = 4
    )
    default boolean stillShowAt200m() {
        return false;
    }

    @ConfigItem(
            keyName = "showGoals",
            name = "Show goals progress",
            description = "Show progress towards the goals you set using the in-game XP menu in addition to progress towards individual levels.",
            position = 6
    )
    default boolean showGoals() {
        return false;
    }

    @ConfigItem(
            keyName = "showOnlyGoals",
            name = "Show ONLY goals progress",
            description = "Hides progress towards individual levels leaving only goals progress.",
            position = 7
    )
    default boolean showOnlyGoals() {
        return false;
    }

    @ConfigItem(
            keyName = "showOnlyOnHover",
            name = "Only show when hovered",
            description = "Only show the progress bars and goals for a skill when it is being hovered over.",
            position = 8
    )
    default boolean showOnHover() {
        return false;
    }

    @ConfigSection(
            name = "Darken",
            description = "Settings for darkening skills as certain thresholds.",
            position = 10
    )
    String SECTION_DARKEN = "Darken";

    String DARKEN_SETTINGS_ENUM_KEY = "darkenType";

    @ConfigItem(
            keyName = DARKEN_SETTINGS_ENUM_KEY,
            name = "Darken skills",
            description = "When, if ever, should a skill stone be darkened?",
            position = 1,
            section = SECTION_DARKEN
    )
    default DarkenType darkenType() {
        return DarkenType.XP200m;
    }

    @ConfigItem(
            keyName = "darkenMembersSkills",
            name = "Always darken members skills",
            description = "In addition to the 'Darken skills' option, should members skills always be darkened?",
            position = 2,
            section = SECTION_DARKEN
    )
    default boolean darkenMembersSkills() {
        return false;
    }

    @Range(
            max = 255
    )
    @ConfigItem(
            keyName = "grayOutOpacity",
            name = "Darken skills opacity",
            description = "Controls how dark skills get when darkened, either by level or XP limits.",
            section = SECTION_DARKEN,
            position = 3
    )
    default int darkenOpacity() {
        return 127;
    }

    @Range(
            max = Experience.MAX_VIRT_LEVEL
    )
    @ConfigItem(
            keyName = "darkenCustomLevel",
            name = "Custom darken level",
            description = "If Darken skills is set to Custom Level, this is the override value to use.",
            section = SECTION_DARKEN,
            position = 4

    )
    default int darkenCustomLevel() {
        return Experience.MAX_REAL_LEVEL;
    }

    @Range(
            max = Experience.MAX_SKILL_XP
    )
    @ConfigItem(
            keyName = "darkenCustomXP",
            name = "Custom darken XP",
            description = "If Darken skills is set to Custom XP, this is the override value to use.",
            section = SECTION_DARKEN,
            position = 5
    )
    default int darkenCustomXP() {
        return Experience.getXpForLevel(Experience.MAX_REAL_LEVEL);
    }

    @ConfigItem(
            keyName = "hideProgressBarWhenDarkened",
            name = "Hide progress bar if darkened",
            description = "If a skill is darkened by a level or XP threshold, this option will also hide its progress bar.",
            section = SECTION_DARKEN,
            position = 6
    )
    default boolean hideProgressBarWhenDarkened() {
        return false;
    }

    @ConfigItem(
            keyName = "hideGoalBarWhenDarkened",
            name = "Hide goal bar if darkened",
            description = "If a skill is darkened by a level or XP threshold, this option will also hide its goal bar.",
            section = SECTION_DARKEN,
            position = 7
    )
    default boolean hideGoalBarWhenDarkened() {
        return false;
    }

    @ConfigSection(
            name = "Bar Colours",
            description = "Settings for level and goal progress bar colours.",
            position = 20
    )
    String SECTION_COLOURS = "Bar Colours";

    @Alpha
    @ConfigItem(
            keyName = "progressBarStartColor",
            name = "Start color",
            description = "The color from which the progress bar fades.",
            section = SECTION_COLOURS,
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
            section = SECTION_COLOURS,
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
            section = SECTION_COLOURS,
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
            section = SECTION_COLOURS,
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
            section = SECTION_COLOURS,
            position = 5
    )
    default Color goalBarEndColor() {
        return new Color(0xFFFF0080);
    }
}

