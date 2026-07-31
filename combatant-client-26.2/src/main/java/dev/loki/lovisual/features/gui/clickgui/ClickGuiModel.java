package dev.loki.lovisual.features.gui.clickgui;

import dev.loki.lovisual.features.gui.clickgui.sections.ClickGuiSection;
import dev.loki.lovisual.render.engine.text.TextRenderer;
import net.minecraft.world.item.ItemStack;

record ClickGuiTabEntry(String id, String label, ClickGuiSection section) {
}

record TextBatchItem(TextRenderer renderer, String text, float x, float y, float size, int argb,
                     boolean shadow) {
}

record TextBatchKey(TextRenderer renderer, int sizeBits, boolean shadow) {
}

record TextMeasureKey(TextRenderer renderer, int sizeBits, String text) {
}

record TextHeightKey(TextRenderer renderer, int sizeBits) {
}

record PickerIcon(ItemStack stack,
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
