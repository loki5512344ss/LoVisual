package dev.loki.lovisual.features.gui.hud.draggable.impl.scripted;

import dev.loki.lovisual.features.gui.hud.draggable.impl.scripted.ScriptedListHudPanel.Panel;
import dev.loki.lovisual.features.gui.hud.draggable.impl.scripted.ScriptedListHudPanel.Palette;
import dev.loki.lovisual.features.gui.hud.draggable.impl.scripted.ScriptedListHudPanel.Variant;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.runtime.CachedUiScriptRuntime;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cache-key signature computation for {@link ScriptedListHudPanel.Panel}: tree, data
 * and layout signatures used by {@link CachedUiScriptRuntime} to decide when the baked
 * UI must be re-generated. Extracted so the panel facade stays under the file-size cap.
 * Sibling folder sharing the parent's package.
 */
final class ScriptedListPanelSignature {

    private ScriptedListPanelSignature() { }

    /** Structure signature: variant geometry, palette, sizing and per-row shape (not text). */
    static long treeSignature(Panel panel) {
        long h = 0xcbf29ce484222325L;
        h = mix(h, panel.variant().id());
        h = mixVariant(h, panel.variant());
        h = mixPalette(h, panel.palette());
        h = mix(h, panel.width());
        h = mix(h, panel.height());
        h = mix(h, panel.drawScale());
        h = mix(h, panel.baseScale());
        h = mix(h, panel.fontScale());
        h = mix(h, panel.headerIconHeight());
        h = mix(h, panel.headerTextHeight());
        h = mix(h, panel.rowTextHeight());
        h = mix(h, panel.countLabelWidth());
        h = mix(h, panel.countValueWidth());
        h = mix(h, panel.blur());
        h = mix(h, panel.blurAlpha());
        h = mix(h, panel.layout());
        h = mix(h, String.valueOf(Math.max(0, panel.activeCount())).length());
        h = mix(h, panel.activeCount());
        List<LinkedHashMap<String, Object>> rows = panel.rows();
        h = mix(h, rows.size());
        for (LinkedHashMap<String, Object> row : rows) {
            h = mix(h, string(row.get("key")));
            h = mix(h, string(row.get("iconKind")));
            h = mix(h, string(row.get("rightText")).length());
            h = mix(h, Math.round(floatValue(row.get("alpha")) * 100.0f));
            Object[] parts = parts(row.get("nameParts"));
            h = mix(h, parts.length);
            for (Object partValue : parts) {
                if (!(partValue instanceof Map<?, ?> part)) continue;
                h = mix(h, string(part.get("text")).length());
                h = mix(h, floatValue(part.get("x")));
            }
        }
        return h;
    }

    /** Content signature: colors, icon assets and visible text that change without a re-bake. */
    static long dataSignature(Panel panel) {
        long h = 0xcbf29ce484222325L;
        h = mix(h, panel.activeCount());
        h = mix(h, panel.headerIconColor());
        for (LinkedHashMap<String, Object> row : panel.rows()) {
            h = mix(h, string(row.get("key")));
            h = mix(h, string(row.get("icon")));
            h = mix(h, string(row.get("iconTint")));
            h = mix(h, string(row.get("rightText")));
            h = mix(h, string(row.get("rightColor")));
            h = mix(h, string(row.get("dividerColor")));
            for (Object partValue : parts(row.get("nameParts"))) {
                if (!(partValue instanceof Map<?, ?> part)) continue;
                h = mix(h, string(part.get("text")));
                h = mix(h, string(part.get("color")));
            }
        }
        return h;
    }

    /** Placement signature: only the panel rectangle, so moving does not change tree/data keys. */
    static long layoutSignature(Panel panel) {
        long h = 0xcbf29ce484222325L;
        h = mix(h, panel.x());
        h = mix(h, panel.y());
        h = mix(h, panel.width());
        h = mix(h, panel.height());
        return h;
    }

    private static long mixVariant(long h, Variant variant) {
        h = mix(h, variant.title());
        h = mix(h, variant.headerIcon());
        h = mix(h, variant.minWidth());
        h = mix(h, variant.headerDividerX());
        h = mix(h, variant.titleIconX());
        h = mix(h, variant.titleTextX());
        h = mix(h, variant.countLabelOffset());
        h = mix(h, variant.countValueOffset());
        h = mix(h, variant.rowIconX());
        h = mix(h, variant.rowIconSize());
        h = mix(h, variant.rowDividerX());
        h = mix(h, variant.rowDividerH());
        h = mix(h, variant.rowTextX());
        h = mix(h, variant.rowRightPad());
        h = mix(h, variant.rowCenterOffset());
        h = mix(h, variant.headerIconFont());
        h = mix(h, variant.headerIconScale());
        return h;
    }

    private static long mixPalette(long h, Palette palette) {
        h = mix(h, palette.headerLeft());
        h = mix(h, palette.headerRight());
        h = mix(h, palette.bodyLeft());
        h = mix(h, palette.bodyRight());
        h = mix(h, palette.outline());
        h = mix(h, palette.text());
        h = mix(h, palette.muted());
        h = mix(h, palette.counter());
        h = mix(h, palette.titleText());
        h = mix(h, palette.divider());
        h = mix(h, palette.blurTint());
        return h;
    }

    private static long mix(long h, String value) { return CachedUiScriptRuntime.mix(h, value); }
    private static long mix(long h, float value) { return CachedUiScriptRuntime.mix(h, value); }
    private static long mix(long h, int value) { return CachedUiScriptRuntime.mix(h, value); }
    private static long mix(long h, boolean value) { return CachedUiScriptRuntime.mix(h, value); }

    private static Object[] parts(Object value) { return value instanceof Object[] arr ? arr : new Object[0]; }
    private static String string(Object value) { return value instanceof String s ? s : ""; }
    private static float floatValue(Object value) { return value instanceof Number n ? n.floatValue() : 0.0f; }
}
