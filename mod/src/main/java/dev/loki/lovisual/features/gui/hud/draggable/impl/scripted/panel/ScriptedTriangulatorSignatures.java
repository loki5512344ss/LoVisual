package dev.loki.lovisual.features.gui.hud.draggable.impl.scripted.panel;

import java.util.LinkedHashMap;
import dev.loki.lovisual.features.gui.hud.draggable.impl.scripted.ScriptedTriangulatorHudPanel;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.runtime.CachedUiScriptRuntime;

/**
 * Cache signatures and element patches for the triangulator HUD panel.
 * Signatures decide whether the baked UI script needs a rebuild.
 */
public final class ScriptedTriangulatorSignatures {

    private ScriptedTriangulatorSignatures() {
    }

    public static long treeSignature(ScriptedTriangulatorHudPanel.Panel panel) {
        long h = 0xcbf29ce484222325L;
        h = CachedUiScriptRuntime.mix(h, panel.width());
        h = CachedUiScriptRuntime.mix(h, panel.height());
        h = CachedUiScriptRuntime.mix(h, panel.drawScale());
        h = CachedUiScriptRuntime.mix(h, panel.baseScale());
        h = CachedUiScriptRuntime.mix(h, panel.fontScale());
        h = CachedUiScriptRuntime.mix(h, panel.headerTextHeight());
        h = CachedUiScriptRuntime.mix(h, panel.rowTextHeight());
        h = CachedUiScriptRuntime.mix(h, panel.countLabelWidth());
        h = CachedUiScriptRuntime.mix(h, panel.countValueWidth());
        h = CachedUiScriptRuntime.mix(h, panel.blur());
        h = CachedUiScriptRuntime.mix(h, panel.blurAlpha());
        h = CachedUiScriptRuntime.mix(h, panel.clearVisible());
        h = CachedUiScriptRuntime.mix(h, panel.copyVisible());
        h = CachedUiScriptRuntime.mix(h, String.valueOf(Math.max(0, panel.activeCount())).length());
        h = CachedUiScriptRuntime.mix(h, panel.activeCount());
        h = CachedUiScriptRuntime.mix(h, panel.statusText().length());
        h = CachedUiScriptRuntime.mix(h, panel.primaryLine().length());
        h = CachedUiScriptRuntime.mix(h, panel.secondaryLine().length());
        h = CachedUiScriptRuntime.mix(h, panel.confidenceText().length());
        h = CachedUiScriptRuntime.mix(h, panel.rows().size());
        for (LinkedHashMap<String, Object> row : panel.rows()) {
            h = CachedUiScriptRuntime.mix(h, string(row.get("key")));
            h = CachedUiScriptRuntime.mix(h, string(row.get("label")).length());
            h = CachedUiScriptRuntime.mix(h, string(row.get("value")).length());
            h = CachedUiScriptRuntime.mix(h, Math.round(floatValue(row.get("alpha")) * 100.0f));
        }
        return h;
    }

    public static long dataSignature(ScriptedTriangulatorHudPanel.Panel panel) {
        long h = 0xcbf29ce484222325L;
        h = mixPalette(h, panel.palette());
        h = CachedUiScriptRuntime.mix(h, panel.activeCount());
        h = CachedUiScriptRuntime.mix(h, panel.headerIcon());
        h = CachedUiScriptRuntime.mix(h, panel.headerIconColor());
        h = CachedUiScriptRuntime.mix(h, panel.clearIconColor());
        h = CachedUiScriptRuntime.mix(h, panel.copyIconColor());
        h = CachedUiScriptRuntime.mix(h, panel.statusText());
        h = CachedUiScriptRuntime.mix(h, panel.primaryLine());
        h = CachedUiScriptRuntime.mix(h, panel.secondaryLine());
        h = CachedUiScriptRuntime.mix(h, panel.confidenceText());
        h = CachedUiScriptRuntime.mix(h, panel.statusColor());
        for (LinkedHashMap<String, Object> row : panel.rows()) {
            h = CachedUiScriptRuntime.mix(h, string(row.get("key")));
            h = CachedUiScriptRuntime.mix(h, string(row.get("label")));
            h = CachedUiScriptRuntime.mix(h, string(row.get("value")));
            h = CachedUiScriptRuntime.mix(h, string(row.get("labelColor")));
            h = CachedUiScriptRuntime.mix(h, string(row.get("valueColor")));
            h = CachedUiScriptRuntime.mix(h, string(row.get("markerColor")));
            h = CachedUiScriptRuntime.mix(h, string(row.get("dividerColor")));
        }
        return h;
    }

    public static long layoutSignature(ScriptedTriangulatorHudPanel.Panel panel) {
        long h = 0xcbf29ce484222325L;
        h = CachedUiScriptRuntime.mix(h, panel.x());
        h = CachedUiScriptRuntime.mix(h, panel.y());
        h = CachedUiScriptRuntime.mix(h, panel.width());
        h = CachedUiScriptRuntime.mix(h, panel.height());
        return h;
    }

    public static LinkedHashMap<String, LinkedHashMap<String, Object>> patches(ScriptedTriangulatorHudPanel.Panel panel) {
        LinkedHashMap<String, LinkedHashMap<String, Object>> patches = new LinkedHashMap<>();
        putPatch(patches, "header:count-value", "text", String.valueOf(Math.max(0, panel.activeCount())));
        putPatch(patches, "header:icon", "tint", ScriptedTriangulatorProps.hex(panel.headerIconColor()));
        putPatch(patches, "clear:icon", "tint", ScriptedTriangulatorProps.hex(panel.clearIconColor()));
        putPatch(patches, "copy:icon", "tint", ScriptedTriangulatorProps.hex(panel.copyIconColor()));
        putPatch(patches, "summary:status", "text", panel.statusText());
        putPatch(patches, "summary:status", "color", ScriptedTriangulatorProps.hex(panel.statusColor()));
        putPatch(patches, "summary:primary", "text", panel.primaryLine());
        putPatch(patches, "summary:secondary", "text", panel.secondaryLine());
        putPatch(patches, "summary:confidence", "text", panel.confidenceText());
        putPatch(patches, "summary:confidence", "color", ScriptedTriangulatorProps.hex(panel.statusColor()));
        for (LinkedHashMap<String, Object> row : panel.rows()) {
            String key = string(row.get("key"));
            String prefix = "row:" + key;
            putPatch(patches, prefix + ":marker", "fill", string(row.get("markerColor")));
            putPatch(patches, prefix + ":divider", "fill", string(row.get("dividerColor")));
            putPatch(patches, prefix + ":label", "text", string(row.get("label")));
            putPatch(patches, prefix + ":label", "color", string(row.get("labelColor")));
            putPatch(patches, prefix + ":value", "text", string(row.get("value")));
            putPatch(patches, prefix + ":value", "color", string(row.get("valueColor")));
        }
        return patches;
    }

    private static long mixPalette(long h, ScriptedTriangulatorHudPanel.Palette palette) {
        h = CachedUiScriptRuntime.mix(h, palette.headerLeft());
        h = CachedUiScriptRuntime.mix(h, palette.headerRight());
        h = CachedUiScriptRuntime.mix(h, palette.bodyLeft());
        h = CachedUiScriptRuntime.mix(h, palette.bodyRight());
        h = CachedUiScriptRuntime.mix(h, palette.outline());
        h = CachedUiScriptRuntime.mix(h, palette.text());
        h = CachedUiScriptRuntime.mix(h, palette.muted());
        h = CachedUiScriptRuntime.mix(h, palette.counter());
        h = CachedUiScriptRuntime.mix(h, palette.titleText());
        h = CachedUiScriptRuntime.mix(h, palette.divider());
        h = CachedUiScriptRuntime.mix(h, palette.blurTint());
        h = CachedUiScriptRuntime.mix(h, palette.success());
        h = CachedUiScriptRuntime.mix(h, palette.warn());
        h = CachedUiScriptRuntime.mix(h, palette.danger());
        return h;
    }

    private static String string(Object value) {
        return value instanceof String s ? s : "";
    }

    private static float floatValue(Object value) {
        return value instanceof Number n ? n.floatValue() : 0.0f;
    }

    private static void putPatch(LinkedHashMap<String, LinkedHashMap<String, Object>> patches,
                                 String key,
                                 String prop,
                                 Object value) {
        LinkedHashMap<String, Object> patch = patches.get(key);
        if (patch == null) {
            patch = new LinkedHashMap<>();
            patches.put(key, patch);
        }
        patch.put(prop, value);
    }
}
