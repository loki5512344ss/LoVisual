package dev.loki.lovisual.features.gui.hud.draggable.impl.scripted;

import java.util.LinkedHashMap;
import dev.loki.lovisual.features.gui.hud.draggable.impl.scripted.panel.ScriptedTriangulatorProps;
import dev.loki.lovisual.features.gui.hud.draggable.impl.scripted.panel.ScriptedTriangulatorSignatures;
import dev.loki.lovisual.features.gui.hud.script.HudScriptLayouts;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.runtime.CachedUiScriptRuntime;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.module.UiScriptModuleHandle;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Triangulator HUD facade: row/prop helpers and the panel records. Rendering
 * lives in {@link ScriptedTriangulatorRenderer}, prop maps and cache
 * signatures in the {@code panel} subpackage.
 */
public final class ScriptedTriangulatorHudPanel {
    private static final String LAYOUT_ID = "lovisual:api/hud/draggable/triangulator";
    private final UiScriptModuleHandle moduleHandle = HudScriptLayouts.handle(LAYOUT_ID);
    private final CachedUiScriptRuntime runtime = new CachedUiScriptRuntime(HudScriptLayouts.runtimeReporter());

    public static String hex(int argb) {
        return ScriptedTriangulatorProps.hex(argb);
    }

    public static LinkedHashMap<String, Object> row(String key,
                                                        String label,
                                                        String value,
                                                        int labelColor,
                                                        int valueColor,
                                                        int markerColor,
                                                        int dividerColor,
                                                        float alpha) {
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("key", key != null ? key : "");
        row.put("label", label != null ? label : "");
        row.put("value", value != null ? value : "");
        row.put("labelColor", hex(labelColor));
        row.put("valueColor", hex(valueColor));
        row.put("markerColor", hex(markerColor));
        row.put("dividerColor", hex(dividerColor));
        row.put("alpha", alpha);
        return row;
    }

    public static String idString(Identifier id) {
        return id != null ? id.toString() : "";
    }

    public static List<LinkedHashMap<String, Object>> rows() {
        return new ArrayList<>();
    }

    public boolean render(Renderer2D renderer,
                   TextRenderer textRenderer,
                   GuiGraphicsExtractor ctx,
                   float tickDelta,
                   Panel panel) {
        return ScriptedTriangulatorRenderer.render(moduleHandle, runtime, renderer, textRenderer, ctx, tickDelta, panel);
    }

    public record Palette(int headerLeft,
                   int headerRight,
                   int bodyLeft,
                   int bodyRight,
                   int outline,
                   int text,
                   int muted,
                   int counter,
                   int titleText,
                   int divider,
                   int blurTint,
                   int success,
                   int warn,
                   int danger) {
        LinkedHashMap<String, Object> toProps() {
            return ScriptedTriangulatorProps.paletteProps(this);
        }
    }

    public record Panel(Palette palette,
                 float x,
                 float y,
                 float width,
                 float height,
                 float drawScale,
                 float baseScale,
                 float fontScale,
                 float headerTextHeight,
                 float rowTextHeight,
                 float countLabelWidth,
                 float countValueWidth,
                 int activeCount,
                 boolean blur,
                 float blurAlpha,
                 String headerIcon,
                 int headerIconColor,
                 boolean clearVisible,
                 int clearIconColor,
                 boolean copyVisible,
                 int copyIconColor,
                 String statusText,
                 String primaryLine,
                 String secondaryLine,
                 String confidenceText,
                 int statusColor,
                 List<LinkedHashMap<String, Object>> rows) {
        public Panel(Palette palette,
              float x,
              float y,
              float width,
              float height,
              float drawScale,
              float baseScale,
              float fontScale,
              float headerTextHeight,
              float rowTextHeight,
              float countLabelWidth,
              float countValueWidth,
              int activeCount,
              boolean blur,
              float blurAlpha,
              String headerIcon,
              int headerIconColor,
              boolean clearVisible,
              int clearIconColor,
              boolean copyVisible,
              int copyIconColor,
              String statusText,
              String primaryLine,
              String secondaryLine,
              String confidenceText,
              int statusColor,
              List<LinkedHashMap<String, Object>> rows) {
            this.palette = palette;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.drawScale = drawScale;
            this.baseScale = baseScale;
            this.fontScale = fontScale;
            this.headerTextHeight = headerTextHeight;
            this.rowTextHeight = rowTextHeight;
            this.countLabelWidth = countLabelWidth;
            this.countValueWidth = countValueWidth;
            this.activeCount = activeCount;
            this.blur = blur;
            this.blurAlpha = blurAlpha;
            this.headerIcon = headerIcon != null ? headerIcon : "";
            this.headerIconColor = headerIconColor;
            this.clearVisible = clearVisible;
            this.clearIconColor = clearIconColor;
            this.copyVisible = copyVisible;
            this.copyIconColor = copyIconColor;
            this.statusText = statusText != null ? statusText : "";
            this.primaryLine = primaryLine != null ? primaryLine : "";
            this.secondaryLine = secondaryLine != null ? secondaryLine : "";
            this.confidenceText = confidenceText != null ? confidenceText : "";
            this.statusColor = statusColor;
            this.rows = rows != null ? rows : List.of();
        }

        LinkedHashMap<String, Object> toProps() {
            return ScriptedTriangulatorProps.panelProps(this);
        }

        long treeSignature() {
            return ScriptedTriangulatorSignatures.treeSignature(this);
        }

        long dataSignature() {
            return ScriptedTriangulatorSignatures.dataSignature(this);
        }

        long layoutSignature() {
            return ScriptedTriangulatorSignatures.layoutSignature(this);
        }

        LinkedHashMap<String, LinkedHashMap<String, Object>> patches() {
            return ScriptedTriangulatorSignatures.patches(this);
        }
    }
}
