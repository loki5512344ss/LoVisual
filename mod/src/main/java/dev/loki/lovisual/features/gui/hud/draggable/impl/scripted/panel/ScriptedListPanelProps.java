package dev.loki.lovisual.features.gui.hud.draggable.impl.scripted;

import dev.loki.lovisual.features.gui.hud.draggable.impl.scripted.ScriptedListHudPanel.Panel;
import dev.loki.lovisual.features.gui.hud.draggable.impl.scripted.ScriptedListHudPanel.Palette;
import dev.loki.lovisual.features.gui.hud.draggable.impl.scripted.ScriptedListHudPanel.Variant;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Property-map and runtime-patch builders for {@link ScriptedListHudPanel}: convert the
 * immutable {@link Panel} description into the string-keyed maps consumed by the scripted
 * UI layout, plus the per-element patch overlay. Extracted so the panel facade stays under
 * the file-size cap. Sibling folder sharing the parent's package.
 */
final class ScriptedListPanelProps {

    private ScriptedListPanelProps() { }

    /** Full property tree for the baked UI: geometry, colors, variant metrics, palette and rows. */
    static LinkedHashMap<String, Object> toProps(Panel panel) {
        Variant variant = panel.variant();
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("id", variant.id());
        out.put("title", variant.title());
        out.put("headerIcon", variant.headerIcon());
        out.put("width", panel.width());
        out.put("height", panel.height());
        out.put("drawScale", panel.drawScale());
        out.put("baseScale", panel.baseScale());
        out.put("fontScale", panel.fontScale());
        out.put("headerIconHeight", panel.headerIconHeight());
        out.put("headerTextHeight", panel.headerTextHeight());
        out.put("rowTextHeight", panel.rowTextHeight());
        out.put("countLabelWidth", panel.countLabelWidth());
        out.put("countValueWidth", panel.countValueWidth());
        out.put("activeCount", panel.activeCount());
        out.put("blur", panel.blur());
        out.put("blurAlpha", panel.blurAlpha());
        out.put("headerIconColor", ScriptedListHudPanel.hex(panel.headerIconColor()));
        out.put("layout", panel.layout());
        out.put("palette", paletteProps(panel.palette()));
        out.put("rows", panel.rows().toArray());
        out.put("variant", variantProps(variant));
        return out;
    }

    static Map<String, Object> variantProps(Variant variant) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("minWidth", variant.minWidth());
        out.put("headerDividerX", variant.headerDividerX());
        out.put("titleIconX", variant.titleIconX());
        out.put("titleTextX", variant.titleTextX());
        out.put("countLabelOffset", variant.countLabelOffset());
        out.put("countValueOffset", variant.countValueOffset());
        out.put("rowIconX", variant.rowIconX());
        out.put("rowIconSize", variant.rowIconSize());
        out.put("rowDividerX", variant.rowDividerX());
        out.put("rowDividerH", variant.rowDividerH());
        out.put("rowTextX", variant.rowTextX());
        out.put("rowRightPad", variant.rowRightPad());
        out.put("rowCenterOffset", variant.rowCenterOffset());
        out.put("headerIconFont", variant.headerIconFont());
        out.put("headerIconScale", variant.headerIconScale());
        return out;
    }

    static LinkedHashMap<String, Object> paletteProps(Palette palette) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("headerLeft", ScriptedListHudPanel.hex(palette.headerLeft()));
        out.put("headerRight", ScriptedListHudPanel.hex(palette.headerRight()));
        out.put("bodyLeft", ScriptedListHudPanel.hex(palette.bodyLeft()));
        out.put("bodyRight", ScriptedListHudPanel.hex(palette.bodyRight()));
        out.put("outline", ScriptedListHudPanel.hex(palette.outline()));
        out.put("text", ScriptedListHudPanel.hex(palette.text()));
        out.put("muted", ScriptedListHudPanel.hex(palette.muted()));
        out.put("counter", ScriptedListHudPanel.hex(palette.counter()));
        out.put("titleText", ScriptedListHudPanel.hex(palette.titleText()));
        out.put("divider", ScriptedListHudPanel.hex(palette.divider()));
        out.put("blurTint", ScriptedListHudPanel.hex(palette.blurTint()));
        return out;
    }

    /** Per-element patch overlay applied after bake: count text, header icon color and row content. */
    static LinkedHashMap<String, LinkedHashMap<String, Object>> patches(Panel panel) {
        LinkedHashMap<String, LinkedHashMap<String, Object>> patches = new LinkedHashMap<>();
        putPatch(patches, "header:count-value", "text", String.valueOf(Math.max(0, panel.activeCount())));
        putPatch(patches, "header:icon", "color", ScriptedListHudPanel.hex(panel.headerIconColor()));
        for (LinkedHashMap<String, Object> row : panel.rows()) {
            String key = string(row.get("key"));
            String rowPrefix = "row:" + key;
            String iconKind = string(row.get("iconKind"));
            String icon = string(row.get("icon"));
            String iconTint = string(row.get("iconTint"));
            if ("item".equals(iconKind)) {
                putPatch(patches, rowPrefix + ":item", "item", icon);
            } else if ("head".equals(iconKind) && !icon.isEmpty()) {
                putPatch(patches, rowPrefix + ":head", "asset", icon);
                putPatch(patches, rowPrefix + ":head", "tint", iconTint);
            } else if ("texture".equals(iconKind) && !icon.isEmpty()) {
                putPatch(patches, rowPrefix + ":texture", "asset", icon);
                putPatch(patches, rowPrefix + ":texture", "tint", iconTint);
            } else if ("svg".equals(iconKind) && !icon.isEmpty()) {
                putPatch(patches, rowPrefix + ":svg", "asset", icon);
                putPatch(patches, rowPrefix + ":svg", "tint", iconTint);
            }
            putPatch(patches, rowPrefix + ":divider", "fill", string(row.get("dividerColor")));
            putPatch(patches, rowPrefix + ":right", "text", string(row.get("rightText")));
            putPatch(patches, rowPrefix + ":right", "color", string(row.get("rightColor")));
            Object[] parts = row.get("nameParts") instanceof Object[] arr ? arr : new Object[0];
            for (int i = 0; i < parts.length; i++) {
                if (!(parts[i] instanceof Map<?, ?> part)) continue;
                String partKey = rowPrefix + ":name:" + i;
                putPatch(patches, partKey, "text", string(part.get("text")));
                putPatch(patches, partKey, "color", string(part.get("color")));
            }
        }
        return patches;
    }

    private static void putPatch(LinkedHashMap<String, LinkedHashMap<String, Object>> patches, String key, String prop, Object value) {
        LinkedHashMap<String, Object> patch = patches.computeIfAbsent(key, ignored -> new LinkedHashMap<>());
        patch.put(prop, value);
    }

    private static String string(Object value) { return value instanceof String s ? s : ""; }
}
