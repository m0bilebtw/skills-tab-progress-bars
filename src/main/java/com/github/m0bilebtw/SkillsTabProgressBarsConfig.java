package com.github.m0bilebtw;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("skillstabprogressbars")
public interface SkillsTabProgressBarsConfig extends Config {

    @ConfigItem(
            keyName = "drawBackgrounds",
            name = "Draw progress bar backgrounds",
            description = "Draw a background behind each progress bar for better visibility",
            position = 1
    )
    default boolean drawBackgrounds() {
        return true;
    }

    @ConfigItem(
            keyName = "transparency",
            name = "Transparency",
            description = "Progress bars and backgrounds will have transparency",
            position = 2
    )
    default boolean transparency() {
        return false;
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
}

