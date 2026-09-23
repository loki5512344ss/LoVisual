package dev.loki.lovisual.features.gui.hud.draggable.impl.combat;

import dev.loki.lovisual.features.gui.hud.registry.HudElementInfo;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElement;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.combat.combatlog.CombatLog;
import dev.loki.lovisual.features.module.modules.combat.combatlog.CombatLogEntry;
import dev.loki.lovisual.features.module.modules.combat.combatlog.CombatLogStats;
import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

/**
 * Draggable panel for {@link CombatLog}: DPS dealt/taken summary plus the last few hits, newest
 * first. Empty (zero width/height) when the module is disabled or nothing has happened yet.
 */
@HudElementInfo(id = "combat_log", displayName = "Combat Log", enabledByDefault = false, order = 200)
public final class CombatLogHud extends DraggableHudElement {

    private static final float PANEL_WIDTH = 140f;
    private static final float ROW_HEIGHT = 11f;
    private static final float PADDING = 5f;
    private static final int MAX_VISIBLE_ROWS = 6;

    @Override
    public void applyDefaultPosition(int screenW, int screenH) {
        this.x = screenW - PANEL_WIDTH - 16f;
        this.y = 16f;
    }

    @Override
    public boolean usesEngineRenderer() {
        return true;
    }

    @Override
    public void renderEngine(Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx,
                             float tickDelta, int screenW, int screenH) {
        CombatLog module = Modules.get(CombatLog.class);
        if (module == null || !module.isEnabled()) {
            width = 0f;
            height = 0f;
            return;
        }

        List<CombatLogEntry> entries = module.getEntries();
        int rows = Math.min(MAX_VISIBLE_ROWS, entries.size());
        width = PANEL_WIDTH;
        height = PADDING * 2f + ROW_HEIGHT * (rows + 2);

        int bg = Theme.theme().windowBg();
        int accent = Theme.theme().accent();
        int textMuted = Theme.theme().textMuted();

        renderer.roundedRect(x, y, width, height, 4f, bg);

        TextRenderer text = Fonts.renderer("Onest", FontInfo.Type.Regular, textRenderer);
        text.begin(1.0, true, false);

        CombatLogStats.Summary summary = module.getSummary();
        String dealtLine = "DPS dealt: " + String.format("%.1f", summary.dpsDealt());
        String takenLine = "DPS taken: " + String.format("%.1f", summary.dpsTaken());
        text.render(dealtLine, x + PADDING, y + PADDING, new RenderColor(accent), false);
        text.render(takenLine, x + PADDING, y + PADDING + ROW_HEIGHT, new RenderColor(textMuted), false);

        float rowY = y + PADDING + ROW_HEIGHT * 2f;
        for (int i = entries.size() - 1, shown = 0; i >= 0 && shown < MAX_VISIBLE_ROWS; i--, shown++) {
            CombatLogEntry entry = entries.get(i);
            String sign = entry.dealtByMe() ? "-" : "+";
            String line = sign + String.format("%.1f", entry.damage()) + "  " + entry.otherName();
            int color = entry.dealtByMe() ? accent : 0xFFE05252;
            text.render(line, x + PADDING, rowY, new RenderColor(color), false);
            rowY += ROW_HEIGHT;
        }

        text.end();
    }
}
