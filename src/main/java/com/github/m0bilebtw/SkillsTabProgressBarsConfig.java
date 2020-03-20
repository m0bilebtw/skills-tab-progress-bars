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

    @Range(
            min = SkillsTabProgressBarsPlugin.MINIMUM_BAR_HEIGHT,
            max = SkillsTabProgressBarsPlugin.MAXIMUM_BAR_HEIGHT
    )
    @ConfigItem(
            keyName = "barHeight",
            name = "Progress bar height",
            description = "The height of the progress bars displayed over the skills tab",
            position = 3
    )
    default int barHeight() {
        return 3;
    }
}

