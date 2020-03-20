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

import static com.github.m0bilebtw.SkillsTabProgressBarsPlugin.MINIMUM_BAR_WIDTH_TO_BE_SEEN_WELL;

@Slf4j
public class SkillsTabProgressBarsOverlay extends Overlay {

    private final Client client;
    private final SkillsTabProgressBarsPlugin plugin;
    private final SkillsTabProgressBarsConfig config;

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

        for (Widget skillWidget : skillsContainer.getStaticChildren()) {
            Skill skill = plugin.skillFromWidgetID(skillWidget.getId());
            if (skill == null) {
                // Skip invalid or unknown skills (includes the skill sidestone and the total level panel)
                continue;
            }

            double thisSkillProgressNormalised = plugin.progressNormalised.getOrDefault(skill, 0d);

            if (thisSkillProgressNormalised != 1d) {
                Rectangle bounds = skillWidget.getBounds();

                final int barWidth = Math.max(MINIMUM_BAR_WIDTH_TO_BE_SEEN_WELL, (int) (thisSkillProgressNormalised * bounds.getWidth()));

                if (config.drawBackgrounds()) {
                    graphics.setColor(config.transparency() ? new Color(0, 0, 0, 127) : Color.BLACK);
                    graphics.fillRect((int) bounds.getX(), (int) (bounds.getY() + bounds.getHeight() - barHeight), (int) bounds.getWidth(), barHeight);
                }

                Color fadedColourNoAlpha = Color.getHSBColor((float) (thisSkillProgressNormalised * 120f) / 360, 1f, 1f);
                graphics.setColor(config.transparency() ?
                        new Color(fadedColourNoAlpha.getColorSpace(), fadedColourNoAlpha.getComponents(null), 0.5f) :
                        fadedColourNoAlpha
                );
                graphics.fillRect((int) bounds.getX(), (int) (bounds.getY() + bounds.getHeight() - barHeight), barWidth, barHeight);
            }
        }
        return null;
    }
}

