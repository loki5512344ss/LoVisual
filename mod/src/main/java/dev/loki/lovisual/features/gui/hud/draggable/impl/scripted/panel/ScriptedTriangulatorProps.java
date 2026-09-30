package dev.loki.lovisual.features.gui.hud.draggable.impl.scripted.panel;

import java.util.LinkedHashMap;
import dev.loki.lovisual.features.gui.hud.draggable.impl.scripted.ScriptedTriangulatorHudPanel;

/**
 * Script prop maps for the triangulator HUD: palette and panel records are
 * converted to the property bags the UI script runtime consumes.
 */
public final class ScriptedTriangulatorProps {

    private ScriptedTriangulatorProps() {
    }

    public static String hex(int argb) {
        return String.format("#%08X", argb);
    }

    public static LinkedHashMap<String, Object> paletteProps(ScriptedTriangulatorHudPanel.Palette palette) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("headerLeft", hex(palette.headerLeft()));
        out.put("headerRight", hex(palette.headerRight()));
        out.put("bodyLeft", hex(palette.bodyLeft()));
        out.put("bodyRight", hex(palette.bodyRight()));
        out.put("outline", hex(palette.outline()));
        out.put("text", hex(palette.text()));
        out.put("muted", hex(palette.muted()));
        out.put("counter", hex(palette.counter()));
        out.put("titleText", hex(palette.titleText()));
        out.put("divider", hex(palette.divider()));
        out.put("blurTint", hex(palette.blurTint()));
        out.put("success", hex(palette.success()));
        out.put("warn", hex(palette.warn()));
        out.put("danger", hex(palette.danger()));
        return out;
    }

    public static LinkedHashMap<String, Object> panelProps(ScriptedTriangulatorHudPanel.Panel panel) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("id", "triangulator");
        out.put("title", "Triangulator");
        out.put("width", panel.width());
        out.put("height", panel.height());
        out.put("drawScale", panel.drawScale());
        out.put("baseScale", panel.baseScale());
        out.put("fontScale", panel.fontScale());
        out.put("headerTextHeight", panel.headerTextHeight());
        out.put("rowTextHeight", panel.rowTextHeight());
        out.put("countLabelWidth", panel.countLabelWidth());
        out.put("countValueWidth", panel.countValueWidth());
        out.put("activeCount", panel.activeCount());
        out.put("blur", panel.blur());
        out.put("blurAlpha", panel.blurAlpha());
        out.put("headerIcon", panel.headerIcon());
        out.put("headerIconColor", hex(panel.headerIconColor()));
        out.put("clearVisible", panel.clearVisible());
        out.put("clearIcon", "x");
        out.put("clearIconColor", hex(panel.clearIconColor()));
        out.put("copyVisible", panel.copyVisible());
        out.put("copyIcon", "clipboard");
        out.put("copyIconColor", hex(panel.copyIconColor()));
        out.put("statusText", panel.statusText());
        out.put("primaryLine", panel.primaryLine());
        out.put("secondaryLine", panel.secondaryLine());
        out.put("confidenceText", panel.confidenceText());
        out.put("statusColor", hex(panel.statusColor()));
        out.put("palette", paletteProps(panel.palette()));
        out.put("rows", panel.rows().toArray());
        return out;
    }
}
