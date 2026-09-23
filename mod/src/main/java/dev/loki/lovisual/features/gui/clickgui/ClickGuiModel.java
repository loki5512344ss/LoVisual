package dev.loki.lovisual.features.gui.clickgui;

import dev.loki.lovisual.features.gui.clickgui.sections.ClickGuiSection;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.world.item.ItemStack;
import dev.loki.lovisual.features.gui.clickgui.ClickGuiModel.ClickGuiTabEntry;
import dev.loki.lovisual.features.gui.clickgui.ClickGuiModel.PickerIcon;
import dev.loki.lovisual.features.gui.clickgui.ClickGuiModel.TextBatchItem;
import dev.loki.lovisual.features.gui.clickgui.ClickGuiModel.TextBatchKey;
import dev.loki.lovisual.features.gui.clickgui.ClickGuiModel.TextHeightKey;
import dev.loki.lovisual.features.gui.clickgui.ClickGuiModel.TextMeasureKey;

public final class ClickGuiModel {

    private ClickGuiModel() {
    }

    public record ClickGuiTabEntry(String id, String label, ClickGuiSection section) {
    }

    public record TextBatchItem(TextRenderer renderer, String text, float x, float y, float size, int argb,
                     boolean shadow) {
    }

    public record TextBatchKey(TextRenderer renderer, int sizeBits, boolean shadow) {
    }

    public record TextMeasureKey(TextRenderer renderer, int sizeBits, String text) {
    }

    public record TextHeightKey(TextRenderer renderer, int sizeBits) {
    }

    public record PickerIcon(ItemStack stack,
                  float x,
                  float y,
                  float scale,
                  boolean hasGlobalClip,
                  float globalClipX,
                  float globalClipY,
                  float globalClipW,
                  float globalClipH,
                  boolean hasClip,
                  float clipX,
                  float clipY,
                  float clipW,
                  float clipH) {
    }
}
