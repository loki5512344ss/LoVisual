package dev.loki.lovisual.features.gui.hud.draggable.impl.scripted;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import dev.loki.lovisual.features.gui.hud.draggable.impl.HudPanelLayoutModes;
import dev.loki.lovisual.features.gui.hud.script.HudScriptLayouts;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.runtime.UiRuntime;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.render.context.UiProjectionMode;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.render.context.UiRenderContext;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.runtime.CachedUiScriptRuntime;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.module.UiScriptModule;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.module.UiScriptModuleHandle;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * Scripted list HUD panel (potions / cooldowns / admins / keybinds). This facade holds the
 * public data model ({@link Variant}, {@link Palette}, {@link Panel}), the shared builders
 * used by the individual renderers and the bake-and-render entry point. Property-map and
 * cache-signature computation live in {@code ScriptedListPanelProps} / {@code ScriptedListPanelSignature}.
 */
public final class ScriptedListHudPanel {
    public static final Variant POTIONS = new Variant(
            "potions", "Potions", String.valueOf((char) 0x42),
            95.0f, 18.0f, 5.0f, 22.0f, 22.0f, 3.0f, 3.5f, 8.0f, 14.0f, 7.0f, 18.0f, 8.0f, 2.1f);
    public static final Variant COOLDOWNS = new Variant(
            "cooldowns", "Cooldowns", "C",
            110.0f, 18.0f, 4.0f, 22.0f, 22.0f, 2.5f, 3.5f, 8.0f, 15.0f, 6.0f, 18.0f, 8.0f, 1.9f);
    public static final Variant ADMINS = new Variant(
            "admins", "Admins", String.valueOf((char) 0x45),
            96.0f, 18.0f, 5.0f, 22.0f, 22.0f, 3.0f, 3.5f, 8.0f, 15.0f, 6.0f, 18.0f, 8.0f, 1.95f);
    public static final Variant KEYBINDS = new Variant(
            "keybinds", "Keybinds", "M",
            102.0f, 18.0f, 5.0f, 22.0f, 22.0f, 3.0f, 3.5f, 8.0f, 15.0f, 6.0f, 18.0f, 8.0f, 1.95f, "Icons", 1.18f);
    public static final float HEADER_HEIGHT = 15.5f;
    public static final float BODY_Y_OFFSET = 18.5f;
    public static final float CONTENT_START_Y = 25.0f;
    public static final float BODY_INSET_Y = 6.5f;
    public static final float ROW_STEP = 11.0f;
    public static final float PANEL_RADIUS = 4.0f;
    public static final float PANEL_STROKE = 0.55f;
    private static final String LAYOUT_ID = "lovisual:api/hud/draggable/list_panel";
    private final UiScriptModuleHandle moduleHandle = HudScriptLayouts.handle(LAYOUT_ID);
    private final CachedUiScriptRuntime runtime = new CachedUiScriptRuntime(HudScriptLayouts.runtimeReporter());

    public static String hex(int argb) {
        return String.format("#%08X", argb);
    }

    public static LinkedHashMap<String, Object> textPart(String text, int color, float x) {
        LinkedHashMap<String, Object> part = new LinkedHashMap<>();
        part.put("text", text != null ? text : "");
        part.put("color", hex(color));
        part.put("x", x);
        return part;
    }

    public static LinkedHashMap<String, Object> row(String key, String iconKind, String icon,
                                                    List<LinkedHashMap<String, Object>> nameParts,
                                                    String rightText, int rightColor, int dividerColor, float alpha) {
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("key", key != null ? key : "");
        row.put("iconKind", iconKind != null ? iconKind : "");
        row.put("icon", icon != null ? icon : "");
        row.put("nameParts", nameParts != null ? nameParts.toArray() : new Object[0]);
        row.put("rightText", rightText != null ? rightText : "");
        row.put("rightColor", hex(rightColor));
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

    public boolean render(Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx,
                          float tickDelta, Panel panel) {
        if (renderer == null || panel == null || panel.width() <= 0.0f || panel.height() <= 0.0f) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getResourceManager() == null) return false;

        HudScriptLayouts.pollReloadCombo(mc);
        if (moduleHandle.consumeChanged()) runtime.reset();

        UiScriptModule module = ensureModule(mc);
        if (module == null) return false;

        TextRenderer fallback = textRenderer != null ? textRenderer : TextRenderer.get();
        LinkedHashMap<String, Object> props = panel.toProps();
        UiRuntime baked = runtime.bake(moduleHandle, module, panel.variant().id(), panel.treeSignature(),
                panel.dataSignature(), panel.layoutSignature(), panel.width(), panel.height(), fallback,
                panel.x(), panel.y(), panel.width(), panel.height(), () -> props, panel::patches);
        if (baked == null) return false;
        baked.render(new UiRenderContext(renderer, fallback, ctx, tickDelta, UiProjectionMode.CURRENT));
        return true;
    }

    private UiScriptModule ensureModule(Minecraft mc) {
        if (!moduleHandle.ensureLoaded(mc.getResourceManager())) {
            HudScriptLayouts.reportLoadError(moduleHandle);
            return null;
        }
        moduleHandle.consumeChanged();
        return moduleHandle.module();
    }

    /** Per-list visual metrics: header/row layout offsets, icon placement and default font settings. */
    public record Variant(String id, String title, String headerIcon, float minWidth, float headerDividerX,
                          float titleIconX, float titleTextX, float countLabelOffset, float countValueOffset,
                          float rowIconX, float rowIconSize, float rowDividerX, float rowDividerH, float rowTextX,
                          float rowRightPad, float rowCenterOffset, String headerIconFont, float headerIconScale) {
        Variant(String id, String title, String headerIcon, float minWidth, float headerDividerX,
                float titleIconX, float titleTextX, float countLabelOffset, float countValueOffset, float rowIconX,
                float rowIconSize, float rowDividerX, float rowDividerH, float rowTextX, float rowRightPad,
                float rowCenterOffset) {
            this(id, title, headerIcon, minWidth, headerDividerX, titleIconX, titleTextX, countLabelOffset,
                    countValueOffset, rowIconX, rowIconSize, rowDividerX, rowDividerH, rowTextX, rowRightPad,
                    rowCenterOffset, "IconsNur", 1.0f);
        }
    }

    /** Panel color scheme as ARGB ints; serialized to hex strings for the scripted UI. */
    public record Palette(int headerLeft, int headerRight, int bodyLeft, int bodyRight, int outline, int text,
                          int muted, int counter, int titleText, int divider, int blurTint) {
        public LinkedHashMap<String, Object> toProps() { return ScriptedListPanelProps.paletteProps(this); }
    }

    /** Immutable render request: variant + palette + geometry + text metrics + row data. */
    public record Panel(Variant variant, Palette palette, float x, float y, float width, float height,
                        float drawScale, float baseScale, float fontScale, float headerIconHeight,
                        float headerTextHeight, float rowTextHeight, float countLabelWidth, float countValueWidth,
                        int activeCount, boolean blur, float blurAlpha, int headerIconColor, String layout,
                        List<LinkedHashMap<String, Object>> rows) {
        public Panel {
            layout = layout != null ? layout : HudPanelLayoutModes.SPLIT_HEADER;
            rows = rows != null ? rows : List.of();
        }

        public LinkedHashMap<String, Object> toProps() { return ScriptedListPanelProps.toProps(this); }

        long treeSignature() { return ScriptedListPanelSignature.treeSignature(this); }

        long dataSignature() { return ScriptedListPanelSignature.dataSignature(this); }

        long layoutSignature() { return ScriptedListPanelSignature.layoutSignature(this); }

        LinkedHashMap<String, LinkedHashMap<String, Object>> patches() { return ScriptedListPanelProps.patches(this); }
    }
}
