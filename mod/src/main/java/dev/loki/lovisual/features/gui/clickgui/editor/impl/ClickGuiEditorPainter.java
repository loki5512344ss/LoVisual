package dev.loki.lovisual.features.gui.clickgui.editor;

import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.helpers.clip.ScissorFunction;

import static dev.loki.lovisual.features.theme.Theme.theme;

/**
 * Draw pass for {@link ClickGuiTextEditorState}: modal chrome, title, multiline
 * text view with selection highlighting, animated caret, vertical scrollbar and
 * the Save / Cancel footer buttons. Also computes the modal + button rectangles
 * from the anchor coordinates. Sibling folder sharing the editor package.
 */
final class ClickGuiEditorPainter {

    private final ClickGuiTextEditorState owner;
    private final ClickGuiEditorCaret caret;

    ClickGuiEditorPainter(ClickGuiTextEditorState owner, ClickGuiEditorCaret caret) {
        this.owner = owner;
        this.caret = caret;
    }

    /** Anchor-relative modal placement, clamped so the dialog never leaves the framebuffer. */
    void layout(int fbw, int fbh) {
        float margin = 12f;
        float w = ClickGuiTextEditorState.WIDTH, h = ClickGuiTextEditorState.HEIGHT, p = ClickGuiTextEditorState.PADDING;
        owner.modalX = ClickGuiEditorCaret.clamp(owner.anchorX - w + owner.anchorW + margin, margin, fbw - w - margin);
        owner.modalY = ClickGuiEditorCaret.clamp(owner.anchorY - h / 2f, margin, fbh - h - margin);
        owner.textX = owner.modalX + p;
        owner.textY = owner.modalY + 58f;
        owner.textW = w - p * 2f;
        owner.textH = h - p * 3f - ClickGuiTextEditorState.BUTTON_H;
        owner.saveW = ClickGuiTextEditorState.BUTTON_W;
        owner.saveH = ClickGuiTextEditorState.BUTTON_H;
        owner.saveX = owner.modalX + w - p - owner.saveW - 14f;
        owner.saveY = owner.modalY + h - p - owner.saveH;
        owner.cancelW = ClickGuiTextEditorState.BUTTON_W;
        owner.cancelH = ClickGuiTextEditorState.BUTTON_H;
        owner.cancelX = owner.saveX - owner.cancelW - 8f;
        owner.cancelY = owner.saveY;
    }

    /** Full paint: modal chrome, text view, selection, caret, scrollbar, footer buttons. */
    void render(int fbw, int fbh) {
        layout(fbw, fbh);
        float w = ClickGuiTextEditorState.WIDTH, h = ClickGuiTextEditorState.HEIGHT;
        float tp = ClickGuiTextEditorState.TEXT_PADDING, lh = ClickGuiTextEditorState.LINE_H;
        ClickGuiRenderer.drawRoundedRect(owner.modalX, owner.modalY, w, h, 12f, theme().windowBg());
        ClickGuiRenderer.drawRoundedRectStroke(owner.modalX, owner.modalY, w, h, 12f, 1.2f, theme().windowStroke());
        TextRenderer titleFont = ClickGuiRenderer.getIosevkaRegular();
        ClickGuiRenderer.drawText(titleFont, owner.title, owner.modalX + ClickGuiTextEditorState.PADDING, owner.modalY + 12f, 16f, theme().textPrimary(), false);
        if (!owner.singleLine) {
            ClickGuiRenderer.drawText(titleFont, "One entry per line", owner.modalX + ClickGuiTextEditorState.PADDING, owner.modalY + 30f, 12f, theme().textMuted(), false);
        }
        ClickGuiRenderer.drawRoundedRect(owner.textX, owner.textY, owner.textW, owner.textH, 8f, theme().surface());
        ClickGuiRenderer.drawRoundedRectStroke(owner.textX, owner.textY, owner.textW, owner.textH, 8f, 1.0f, theme().strokeSoft());

        String[] lines = owner.buffer.toString().split("\\n", -1);
        float fontSize = 15f;
        float viewX = owner.textX + tp - owner.scrollX;
        float viewY = owner.textY + tp + owner.scroll;
        float lineTop = viewY;
        float scissorX = owner.textX + tp, scissorY = owner.textY + tp;
        float scissorW = owner.textW - tp * 2f, scissorH = owner.textH - tp * 2f;

        boolean clipped = ScissorFunction.pushRaw(scissorX, scissorY, scissorW, scissorH);
        int globalOffset = 0;
        for (int idx = 0; idx < lines.length; idx++) {
            String line = lines[idx];
            drawSelectionForLine(line, globalOffset, viewX, lineTop, fontSize, tp, lh);
            float textYLine = lineTop + (lh - ClickGuiRenderer.textHeight(titleFont, fontSize)) * 0.5f;
            ClickGuiRenderer.drawText(titleFont, line, viewX, textYLine, fontSize, theme().textPrimary(), false);
            lineTop += lh;
            globalOffset += line.length();
            if (idx < lines.length - 1) globalOffset++;
        }
        drawCaret(lines, viewX, viewY, titleFont, fontSize, lh);
        if (clipped) ScissorFunction.pop();
        drawScrollbar(scissorX, scissorY, scissorW, scissorH, lines.length * lh);
        drawFooter(titleFont);
    }

    private void drawSelectionForLine(String line, int globalOffset, float viewX, float lineTop, float fontSize, float tp, float lh) {
        if (!owner.selection.appliesToLine(0) || !owner.selection.hasRange()) return;
        int selStart = Math.min(owner.selection.start(), owner.selection.end());
        int selEnd = Math.max(owner.selection.start(), owner.selection.end()) + 1;
        int selA = Math.max(globalOffset, selStart);
        int selB = Math.min(globalOffset + line.length(), selEnd);
        if (selB <= selA) return;
        int colA = selA - globalOffset, colB = selB - globalOffset;
        float x0 = viewX + ClickGuiRenderer.textWidth(ClickGuiRenderer.getIosevkaRegular(), ClickGuiEditorCaret.safeSubstring(line, 0, colA), fontSize);
        float x1 = viewX + ClickGuiRenderer.textWidth(ClickGuiRenderer.getIosevkaRegular(), ClickGuiEditorCaret.safeSubstring(line, 0, colB), fontSize);
        ClickGuiRenderer.drawRoundedRect(x0, lineTop, Math.max(1f, x1 - x0), lh, 3f, theme().accentSoft());
    }

    private void drawCaret(String[] lines, float viewX, float viewY, TextRenderer font, float fontSize, float lh) {
        int[] pos = ClickGuiEditorCaret.caretLineAndColumn(lines, owner.caret);
        float caretLineTop = viewY + pos[0] * lh;
        String caretLine = pos[0] < lines.length ? lines[pos[0]] : "";
        float caretOffsetX = ClickGuiRenderer.textWidth(font, ClickGuiEditorCaret.safeSubstring(caretLine, 0, pos[1]), fontSize);
        float cx = viewX + caretOffsetX;
        if (AnimationUtility.blink(500L)) ClickGuiRenderer.drawLine(cx, caretLineTop + 2f, cx, caretLineTop + lh - 2f, theme().textPrimary());
    }

    private void drawScrollbar(float scissorX, float scissorY, float scissorW, float scissorH, float contentHeight) {
        if (contentHeight <= scissorH + 0.5f) return;
        float trackX = scissorX + scissorW - 6f, trackY = scissorY, trackW = 4f, trackH = scissorH;
        float thumbH = Math.max(18f, trackH * (scissorH / contentHeight));
        float maxScroll = contentHeight - scissorH;
        float scrollNorm = ClickGuiEditorCaret.clamp(-owner.scroll / maxScroll, 0f, 1f);
        float thumbY = trackY + (trackH - thumbH) * scrollNorm;
        ClickGuiRenderer.drawRoundedRect(trackX, trackY, trackW, trackH, 3f, theme().surface());
        ClickGuiRenderer.drawRoundedRect(trackX, thumbY, trackW, thumbH, 3f, theme().strokeSoft());
    }

    private void drawFooter(TextRenderer font) {
        float mx = ClickGuiRenderer.getMouseX(), my = ClickGuiRenderer.getMouseY();
        boolean hoverCancel = caret.inside(mx, my, owner.cancelX, owner.cancelY, owner.cancelW, owner.cancelH);
        boolean hoverSave = caret.inside(mx, my, owner.saveX, owner.saveY, owner.saveW, owner.saveH);
        int cancelFill = hoverCancel ? theme().surfaceHover() : theme().surface();
        int saveFill = hoverSave ? ClickGuiRenderer.mixColor(theme().accent(), theme().surfaceHover(), 0.35f) : theme().accent();
        ClickGuiRenderer.drawRoundedRect(owner.cancelX, owner.cancelY, owner.cancelW, owner.cancelH, 8f, cancelFill);
        ClickGuiRenderer.drawRoundedRectStroke(owner.cancelX, owner.cancelY, owner.cancelW, owner.cancelH, 8f, 1.05f, theme().strokeSoft());
        ClickGuiRenderer.drawText(font, "Cancel", owner.cancelX + 14f, owner.cancelY + (owner.cancelH - ClickGuiRenderer.textHeight(font, 14f)) * 0.5f, 14f, theme().textPrimary(), false);
        ClickGuiRenderer.drawRoundedRect(owner.saveX, owner.saveY, owner.saveW, owner.saveH, 8f, saveFill);
        ClickGuiRenderer.drawRoundedRectStroke(owner.saveX, owner.saveY, owner.saveW, owner.saveH, 8f, 1.05f, theme().strokeSoft());
        ClickGuiRenderer.drawText(font, "Save", owner.saveX + 20f, owner.saveY + (owner.saveH - ClickGuiRenderer.textHeight(font, 14f)) * 0.5f, 14f, theme().textPrimary(), false);
    }
}
