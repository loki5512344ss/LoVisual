package dev.loki.lovisual.features.gui.clickgui.settings.mode;

import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.config.style.StyleConfig;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.util.wav.util.ClickGuiSounds;
import dev.loki.lovisual.features.gui.clickgui.settings.skin.SettingSkinUtil;
import dev.loki.lovisual.features.gui.clickgui.settings.skin.UnifiedSettingsSkin;
import dev.loki.lovisual.features.gui.clickgui.settings.mode.ModeSetting;

public final class ModeSettingRenderer {
    /** Visual sample tiles shown under the surface-style row (AUTO has no sample). */
    private static final String[] SWATCH_IDS = {"glass", "blur", "flat", "outline", "none"};

    private ModeSettingRenderer() {
    }

    private static boolean isSurfaceStyle(ModeSetting setting) {
        return "surfacestyle".equals(setting.getId()) && setting.getParent() instanceof StyleConfig;
    }

    public static void render(ModeSetting setting, float x, float y, float w, float mx, float my) {
        UnifiedSettingsSkin.syncTheme();
        ModeSetting.UiState ui = setting.ui();
        ui.baseX = x;
        ui.baseY = y;
        ui.baseW = w;
        layoutMode(setting);

        TextRenderer font = UnifiedSettingsSkin.fontMedium();
        float size = SettingSkinUtil.m(17f, 6.5f);
        ClickGuiRenderer.drawText(font, setting.getDisplayName(), x + SettingSkinUtil.m(5f, 5f), y + SettingSkinUtil.m(4f, 1.3f), size, UnifiedSettingsSkin.TEXT_PRIMARY, false);
        SettingSkinUtil.renderOptionContainer(x + SettingSkinUtil.m(5f, 5f), y + SettingSkinUtil.m(24f, 9.7f), ui.selectW, optionContainerHeightMode(ui), false);

        for (ModeSetting.OptionLayout opt : ui.optionLayouts) {
            float ox = x + opt.x();
            float oy = y + opt.y();
            boolean active = opt.getId().equals(setting.selectedId());
            boolean hover = UnifiedSettingsSkin.inside(mx, my, ox - SettingSkinUtil.m(2f, 2f), oy - SettingSkinUtil.m(1f, 1f), opt.w() + SettingSkinUtil.m(4f, 4f), SettingSkinUtil.m(16f, 8f));
            float hoverAnim = AnimationUtility.approach(ui.hoverAnims.getOrDefault(opt.getId(), 0f), hover ? 1f : 0f, 0.20f);
            ui.hoverAnims.put(opt.getId(), hoverAnim);
            int color = active ? UnifiedSettingsSkin.ACCENT : UnifiedSettingsSkin.mix(UnifiedSettingsSkin.TEXT_MUTED, UnifiedSettingsSkin.TEXT_PRIMARY, hoverAnim * 0.50f);
            ClickGuiRenderer.drawText(font, opt.label(), ox, oy, size, color, false);
        }
        renderSwatches(setting, x, y, mx, my);
    }

    public static void mouseClicked(ModeSetting setting, double mx, double my, int button) {
        if (button != 0) return;
        ModeSetting.UiState ui = setting.ui();
        if (isSurfaceStyle(setting)) {
            for (ModeSetting.SwatchLayout sw : ui.swatchLayouts) {
                float ox = ui.baseX + sw.x();
                float oy = ui.baseY + sw.y();
                if (!UnifiedSettingsSkin.inside(mx, my, ox, oy, sw.w(), sw.h())) continue;
                applySelection(setting, sw.id(), setting.selectedId());
                return;
            }
        }
        for (ModeSetting.OptionLayout opt : ui.optionLayouts) {
            float ox = ui.baseX + opt.x();
            float oy = ui.baseY + opt.y();
            if (!UnifiedSettingsSkin.inside(mx, my, ox - SettingSkinUtil.m(2f, 2f), oy - SettingSkinUtil.m(1f, 1f), opt.w() + SettingSkinUtil.m(4f, 4f), SettingSkinUtil.m(16f, 8f)))
                continue;
            applySelection(setting, opt.getId(), setting.selectedId());
            return;
        }
    }

    private static void applySelection(ModeSetting setting, String id, String before) {
        setting.selectId(id);
        if (!id.equals(before)) ClickGuiSounds.changeMode();
        if (setting.onChange() != null) setting.onChange().run();
        if (setting.getParent() != null) setting.getParent().saveConfig();
    }

    public static float getHeight(ModeSetting setting) {
        layoutMode(setting);
        return setting.ui().cachedHeight;
    }

    private static void layoutMode(ModeSetting setting) {
        ModeSetting.UiState ui = setting.ui();
        float width = ui.baseW > 0f ? ui.baseW : SettingSkinUtil.m(160f, 115f);
        int count = setting.optionIds().size();
        if (Float.isFinite(ui.lastLayoutW) && Math.abs(ui.lastLayoutW - width) <= 0.5f && ui.layoutOptionCount == count)
            return;
        ui.optionLayouts.clear();
        TextRenderer font = UnifiedSettingsSkin.fontMedium();
        float size = SettingSkinUtil.m(17f, 6.5f);
        float maxW = Math.max(1f, width - SettingSkinUtil.m(10f, 10f));
        float offsetX = 0f, offsetY = 0f, maxRowW = 0f;
        for (String id : setting.optionIds()) {
            String label = setting.getOptionDisplayName(id);
            float tw = UnifiedSettingsSkin.textWidth(font, label, size) + SettingSkinUtil.m(2f, 2f);
            if (offsetX + tw >= maxW && offsetX > 0f) {
                maxRowW = Math.max(maxRowW, offsetX);
                offsetX = 0f;
                offsetY += SettingSkinUtil.m(16f, 8f);
            }
            ui.optionLayouts.add(new ModeSetting.OptionLayout(id, label, SettingSkinUtil.m(7f, 6f) + offsetX, SettingSkinUtil.m(27f, 10.7f) + offsetY, tw));
            offsetX += tw;
        }
        maxRowW = Math.max(maxRowW, offsetX);
        ui.selectW = Math.max(0f, maxRowW + SettingSkinUtil.m(2f, 2f));
        layoutSwatches(setting, ui, offsetY);
        if (ui.swatchLayouts.isEmpty()) {
            ui.cachedHeight = SettingSkinUtil.m(45f, 22f) + offsetY;
        } else {
            ModeSetting.SwatchLayout last = ui.swatchLayouts.get(ui.swatchLayouts.size() - 1);
            ui.cachedHeight = last.y() + last.h() + SettingSkinUtil.m(15f, 7f);
        }
        ui.lastLayoutW = width;
        ui.layoutOptionCount = count;
    }

    private static void layoutSwatches(ModeSetting setting, ModeSetting.UiState ui, float offsetY) {
        ui.swatchLayouts.clear();
        if (!isSurfaceStyle(setting)) return;
        float sw = SettingSkinUtil.m(30f, 14f);
        float sh = SettingSkinUtil.m(16f, 7f);
        float gap = SettingSkinUtil.m(4f, 3f);
        float rowY = SettingSkinUtil.m(27f, 10.7f) + offsetY + optionContainerHeightMode(ui) + SettingSkinUtil.m(5f, 4f);
        float sx = SettingSkinUtil.m(7f, 6f);
        for (String id : SWATCH_IDS) {
            ui.swatchLayouts.add(new ModeSetting.SwatchLayout(id, sx, rowY, sw, sh));
            sx += sw + gap;
        }
    }

    private static void renderSwatches(ModeSetting setting, float x, float y, float mx, float my) {
        ModeSetting.UiState ui = setting.ui();
        if (ui.swatchLayouts.isEmpty()) return;
        TextRenderer font = UnifiedSettingsSkin.fontMedium();
        float size = SettingSkinUtil.m(11f, 5f);
        float radius = SettingSkinUtil.m(3f, 3f);
        for (ModeSetting.SwatchLayout sw : ui.swatchLayouts) {
            float ox = x + sw.x();
            float oy = y + sw.y();
            boolean active = sw.id().equals(setting.selectedId());
            boolean hover = UnifiedSettingsSkin.inside(mx, my, ox, oy, sw.w(), sw.h());
            float hoverAnim = AnimationUtility.approach(ui.hoverAnims.getOrDefault(sw.id(), 0f), hover ? 1f : 0f, 0.20f);
            ui.hoverAnims.put(sw.id(), hoverAnim);
            drawSwatchSample(sw.id(), ox, oy, sw.w(), sw.h(), radius, hoverAnim);
            int stroke = active
                    ? UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.ACCENT, 210)
                    : UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.TEXT_FAINT, 70 + Math.round(hoverAnim * 70f));
            ClickGuiRenderer.drawRoundedRectStroke(ox, oy, sw.w(), sw.h(), radius, SettingSkinUtil.m(0.8f, 0.5f), stroke);
            String label = setting.getOptionDisplayName(sw.id());
            float tw = UnifiedSettingsSkin.textWidth(font, label, size);
            int color = active
                    ? UnifiedSettingsSkin.ACCENT
                    : UnifiedSettingsSkin.mix(UnifiedSettingsSkin.TEXT_MUTED, UnifiedSettingsSkin.TEXT_PRIMARY, 0.55f + hoverAnim * 0.45f);
            ClickGuiRenderer.drawText(font, label, ox + Math.max(0f, (sw.w() - tw) * 0.5f), oy + sw.h() + SettingSkinUtil.m(3f, 1.5f), size, color, false);
        }
    }

    private static void drawSwatchSample(String id, float x, float y, float w, float h, float radius, float hoverAnim) {
        switch (id) {
            case "glass" -> {
                ClickGuiRenderer.drawRoundedRect(x, y, w, h, radius, 0x33FFFFFF);
                ClickGuiRenderer.drawRoundedRect(x + w * 0.08f, y + h * 0.12f, w * 0.84f, h * 0.34f, radius, 0x40FFFFFF);
            }
            case "blur" -> {
                ClickGuiRenderer.drawRoundedRect(x, y, w, h, radius, 0x4D8A8A96);
                ClickGuiRenderer.drawRoundedRect(x, y + h * 0.55f, w, h * 0.30f, radius, 0x3360606E);
            }
            case "flat" -> ClickGuiRenderer.drawRoundedRect(x, y, w, h, radius, 0xFF3A3A46);
            case "outline" -> {
            }
            case "none" -> ClickGuiRenderer.drawRoundedRect(x, y, w, h, radius, 0x14000000);
            default -> {
            }
        }
        if (hoverAnim > 0.01f && ("outline".equals(id) || "none".equals(id))) {
            ClickGuiRenderer.drawRoundedRect(x, y, w, h, radius,
                    UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.SURFACE_HOVER, Math.round(95f * hoverAnim)));
        }
    }

    private static float optionContainerHeightMode(ModeSetting.UiState ui) {
        float max = SettingSkinUtil.m(10f, 10f);
        for (ModeSetting.OptionLayout opt : ui.optionLayouts) max = Math.max(max, opt.y() - SettingSkinUtil.m(19f, 9.7f) + SettingSkinUtil.m(16f, 8f));
        return max;
    }
}
