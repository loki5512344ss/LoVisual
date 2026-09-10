package dev.loki.lovisual.features.module.modules.visuals.cosmetics;

import dev.loki.lovisual.features.cosmetic.manager.CosmeticManager;
import dev.loki.lovisual.features.cosmetic.model.CosmeticModel;
import dev.loki.lovisual.features.cosmetic.manager.CosmeticRepository;
import dev.loki.lovisual.features.cosmetic.model.CosmeticSlot;
import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import net.minecraft.client.resources.language.I18n;

import java.util.List;
import dev.loki.lovisual.features.module.modules.visuals.cosmetics.CosmeticDisplay;
import dev.loki.lovisual.features.module.modules.visuals.cosmetics.CosmeticsScreen;

/**
 * Right panel of {@link CosmeticsScreen}: model picker for the currently selected
 * slot (live-apply on click, "None" always first) plus, for accessory slots, the
 * five per-slot offset/rotation/scale sliders declared on {@link CosmeticDisplay}.
 */
public final class CosmeticsSlotPanel {
    private static final float ROW_HEIGHT = 20f;
    private static final float SLIDER_HEIGHT = 14f;

    private final CosmeticDisplay module;

    CosmeticsSlotPanel(CosmeticDisplay module) {
        this.module = module;
    }

    void render(float x, float y, float width, float height, CosmeticSlot slot, double mouseX, double mouseY) {
        TextRenderer text = Fonts.renderer("Onest", FontInfo.Type.Regular, TextRenderer.get());
        int accent = Theme.theme().accent();
        int muted = Theme.theme().textMuted();

        text.begin(1.0, true, false);
        text.render(slot.getDisplayName(), x, y, new RenderColor(Theme.theme().textPrimary()), false);
        text.end();

        float rowY = y + 16f;
        rowY = renderRow(x, rowY, width, "None", isNoneActive(slot), mouseX, mouseY, text, accent, muted);

        for (CosmeticModel model : CosmeticRepository.get().bySlot(slot)) {
            boolean active = CosmeticManager.get().getActive(slot) == model;
            rowY = renderRow(x, rowY, width, model.getName(), active, mouseX, mouseY, text, accent, muted);
        }

        if (slot.isAccessory()) {
            renderOffsetSliders(x, rowY + 10f, width, slot, mouseX, mouseY, text, accent, muted);
        }
    }

    /** @return the y position immediately below the rendered row. */
    private float renderRow(float x, float y, float width, String label, boolean active,
                            double mouseX, double mouseY, TextRenderer text, int accent, int muted) {
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + ROW_HEIGHT;
        int bg = active ? Theme.theme().accentSoft() : hovered ? Theme.theme().surfaceHover() : Theme.theme().surface();
        Renderer2D.COLOR.roundedRect(x, y, width, ROW_HEIGHT - 2f, 3f, bg);

        text.begin(1.0, true, false);
        text.render(label, x + 6f, y + 6f, new RenderColor(active ? accent : muted), false);
        text.end();

        return y + ROW_HEIGHT;
    }

    private void renderOffsetSliders(float x, float y, float width, CosmeticSlot slot,
                                     double mouseX, double mouseY, TextRenderer text, int accent, int muted) {
        CosmeticDisplay.SlotOffsetSettings offsets = module.offsetsFor(slot);
        if (offsets == null) return;

        float rowY = y;
        rowY = slider(x, rowY, width, I18n.get("setting.cosmetics.offset_x"), offsets.offsetX(), text, accent, muted);
        rowY = slider(x, rowY, width, I18n.get("setting.cosmetics.offset_y"), offsets.offsetY(), text, accent, muted);
        rowY = slider(x, rowY, width, I18n.get("setting.cosmetics.offset_z"), offsets.offsetZ(), text, accent, muted);
        rowY = slider(x, rowY, width, I18n.get("setting.cosmetics.rotation"), offsets.rotation(), text, accent, muted);
        slider(x, rowY, width, I18n.get("setting.cosmetics.scale"), offsets.scale(), text, accent, muted);
    }

    private float slider(float x, float y, float width, String label,
                         dev.loki.lovisual.config.values.primitive.NumberValue<Float> value,
                         TextRenderer text, int accent, int muted) {
        text.begin(0.85, true, false);
        text.render(label + ": " + String.format("%.2f", value.get()), x, y, new RenderColor(muted), false);
        text.end();

        float barY = y + 10f;
        float min = value.getMin();
        float max = value.getMax();
        float fraction = max > min ? (value.get() - min) / (max - min) : 0f;

        Renderer2D.COLOR.roundedRect(x, barY, width, SLIDER_HEIGHT - 4f, 2f, Theme.theme().surface());
        Renderer2D.COLOR.roundedRect(x, barY, width * fraction, SLIDER_HEIGHT - 4f, 2f, accent);

        return barY + SLIDER_HEIGHT;
    }

    /**
     * Drag/click on a slider bar; call from the screen's mouse handlers with the same
     * rect origin used in {@link #render}. Returns true if the point hit a slider for this slot.
     */
    boolean handleSliderDrag(float x, float listBottomY, float width, CosmeticSlot slot, double mouseX, double mouseY) {
        if (!slot.isAccessory()) return false;
        CosmeticDisplay.SlotOffsetSettings offsets = module.offsetsFor(slot);
        if (offsets == null) return false;

        List<dev.loki.lovisual.config.values.primitive.NumberValue<Float>> sliders = List.of(
                offsets.offsetX(), offsets.offsetY(), offsets.offsetZ(), offsets.rotation(), offsets.scale());

        float rowY = listBottomY + 10f;
        for (dev.loki.lovisual.config.values.primitive.NumberValue<Float> value : sliders) {
            float barY = rowY + 10f;
            if (mouseY >= barY && mouseY <= barY + SLIDER_HEIGHT - 4f && mouseX >= x && mouseX <= x + width) {
                float fraction = (float) Math.clamp((mouseX - x) / width, 0.0, 1.0);
                value.set(value.getMin() + fraction * (value.getMax() - value.getMin()));
                return true;
            }
            rowY += SLIDER_HEIGHT;
        }
        return false;
    }

    boolean handleClick(float x, float y, float width, CosmeticSlot slot, double mouseX, double mouseY) {
        List<CosmeticModel> models = CosmeticRepository.get().bySlot(slot);
        float rowY = y + 16f;

        if (rowHit(x, rowY, width, mouseX, mouseY)) {
            CosmeticManager.get().setActive(slot, null);
            return true;
        }
        rowY += ROW_HEIGHT;

        for (CosmeticModel model : models) {
            if (rowHit(x, rowY, width, mouseX, mouseY)) {
                CosmeticManager.get().setActive(slot, model);
                return true;
            }
            rowY += ROW_HEIGHT;
        }

        return handleSliderDrag(x, rowY, width, slot, mouseX, mouseY);
    }

    private boolean rowHit(float x, float y, float width, double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + ROW_HEIGHT;
    }

    private boolean isNoneActive(CosmeticSlot slot) {
        return CosmeticManager.get().getActive(slot) == null;
    }
}
