package com.github.m0bilebtw;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;

@Slf4j
public class SkillsTabProgressBarsOverlay extends Overlay {

    private final Client client;
    private final SkillsTabProgressBarsPlugin plugin;
    private final SkillsTabProgressBarsConfig config;

    static final int MINIMUM_BAR_WIDTH_TO_BE_SEEN_WELL = 2;
    static final int INDENT_WIDTH_ONE_SIDE = 4; // The skill panel from OSRS indents 3 pixels at the bottom (and top)

    @Inject
    public SkillsTabProgressBarsOverlay(Client client, SkillsTabProgressBarsPlugin plugin, SkillsTabProgressBarsConfig config) {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        Widget skillsContainer = client.getWidget(WidgetInfo.SKILLS_CONTAINER);
        if (skillsContainer == null || skillsContainer.isHidden()) {
            return null;
        }

        final int barHeight = config.barHeight();
        final boolean indent = config.indent();
        final Skill hoveredSkill = SkillsTabProgressBarsPlugin.hoveredSkill;

        for (Widget skillWidget : skillsContainer.getStaticChildren()) {
            Skill skill = plugin.skillFromWidgetID(skillWidget.getId());
            if (skill == null || skill == Skill.OVERALL) {
                // Skip invalid or unknown skills (includes the skill sidestone and the total level panel)
                continue;
            }
            if (hoveredSkill != null && hoveredSkill != skill) {
                // Skip if hovering a skill that isn't this one
                continue;
            }

            double thisSkillProgressToLevelNormalised = plugin.progressToLevelNormalised.getOrDefault(skill, 1d);
            if (thisSkillProgressToLevelNormalised < 1d) {
                // Actually draw widgets that get here
                Rectangle bounds = skillWidget.getBounds();
                final int effectiveBoundsWidth = (int) bounds.getWidth() - (indent ? 2 * INDENT_WIDTH_ONE_SIDE : 0);
                final int barWidthToLevel = Math.max(MINIMUM_BAR_WIDTH_TO_BE_SEEN_WELL, (int) (thisSkillProgressToLevelNormalised * effectiveBoundsWidth));
                final int barStartX = (int) bounds.getX() + (indent ? INDENT_WIDTH_ONE_SIDE : 0);

                if (config.drawBackgrounds()) {
                    graphics.setColor(config.transparency() ? new Color(0, 0, 0, 127) : Color.BLACK);
                    graphics.fillRect(barStartX, (int) (bounds.getY() + bounds.getHeight() - barHeight), effectiveBoundsWidth, barHeight);
                }

                Color fadedColourNoAlpha = Color.getHSBColor((float) (thisSkillProgressToLevelNormalised * 120f) / 360, 1f, 1f);
                graphics.setColor(config.transparency() ?
                        new Color(fadedColourNoAlpha.getColorSpace(), fadedColourNoAlpha.getComponents(null), 0.5f) :
                        fadedColourNoAlpha
                );
                graphics.fillRect(barStartX, (int) (bounds.getY() + bounds.getHeight() - barHeight), barWidthToLevel, barHeight);
            }
        }
        return null;
    }
}

