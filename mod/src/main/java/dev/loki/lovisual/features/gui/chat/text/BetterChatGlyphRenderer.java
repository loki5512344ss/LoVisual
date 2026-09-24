package dev.loki.lovisual.features.gui.chat.text;

import dev.loki.lovisual.features.command.util.CommandOutput;
import dev.loki.lovisual.render.engine.svg.model.SvgRenderOptions;
import dev.loki.lovisual.render.engine.text.glyph.custom.TextGlyphFallback;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.util.time.GameClock;
import net.minecraft.network.chat.Component;

import java.util.List;
import dev.loki.lovisual.features.gui.chat.messages.BetterChatMessageRenderer;
import dev.loki.lovisual.features.gui.chat.model.BetterChatModel;
import dev.loki.lovisual.features.gui.chat.render.BetterChatRenderer;
import dev.loki.lovisual.features.gui.chat.text.util.BetterChatDrawUtil;
import dev.loki.lovisual.features.gui.chat.text.util.BetterChatFontUtil;

/**
 * Glyph + command-prefix gradient rendering for laid-out frame lines.
 */
public final class BetterChatGlyphRenderer {
    private BetterChatGlyphRenderer() {
    }

    public static void renderGlyphs(List<BetterChatModel.FrameLine> lines, float fontSize, float lineHeight, boolean activeChatSurface) {
        if (lines == null || lines.isEmpty()) return;
        float scale = BetterChatFontUtil.scaleForSize(fontSize);
        TextRenderer current = null;
        float currentHeight = 0f;
        float shadowOffset = Math.max(1f, fontSize * (activeChatSurface ? 0.06f : 0.08f));
        float shadowAlpha = activeChatSurface ? 0.48f : 0.68f;
        for (BetterChatModel.FrameLine line : lines) {
            float alpha = activeChatSurface ? 1f : BetterChatMessageRenderer.fade(line.message().ageSeconds());
            if (alpha <= 0.01f) continue;
            float baseY = line.y0();
            float reveal = BetterChatMessageRenderer.newMessageReveal(line.message());
            float xOffset = -(1f - reveal) * BetterChatRenderer.NEW_MESSAGE_SLIDE_PX;
            boolean commandLine = CommandOutput.isLoVisualMessage(line.message().text());
            List<BetterChatModel.GlyphBox> glyphs = line.glyphs();
            for (int gi = 0; gi < glyphs.size(); gi++) {
                BetterChatModel.GlyphBox g = glyphs.get(gi);
                if (commandLine && g.charIndex() < CommandOutput.PREFIX.length() && !TextGlyphFallback.isSvgFontKey(g.font())) {
                    if (current != null) {
                        current.end();
                        current = null;
                    }
                    int runEnd = gi + 1;
                    while (runEnd < glyphs.size()) {
                        BetterChatModel.GlyphBox next = glyphs.get(runEnd);
                        if (next.charIndex() >= CommandOutput.PREFIX.length()) break;
                        if (TextGlyphFallback.isSvgFontKey(next.font())) break;
                        if (!java.util.Objects.equals(next.font(), g.font())) break;
                        runEnd++;
                    }
                    renderCommandPrefixRun(glyphs, gi, runEnd, baseY, lineHeight, xOffset, scale, alpha, shadowOffset, shadowAlpha, line.message().text());
                    gi = runEnd - 1;
                    continue;
                }
                if (TextGlyphFallback.isSvgFontKey(g.font())) {
                    if (current != null) {
                        current.end();
                        current = null;
                    }
                    String svgName = TextGlyphFallback.svgNameFromFontKey(g.font());
                    float gx = g.x0() + xOffset;
                    float iconSize = Math.max(1.0f, g.x1() - g.x0());
                    float iconY = baseY + (lineHeight - iconSize) * 0.5f;
                    if (svgName != null) {
                        BetterChatRenderer.renderer.svg(svgName, gx, iconY, iconSize, iconSize, SvgRenderOptions.fromFile().withAlpha(alpha));
                    }
                    continue;
                }
                TextRenderer tr = BetterChatFontUtil.fontRenderer(g.font());
                if (tr != current) {
                    if (current != null) {
                        current.end();
                    }
                    current = tr;
                    current.begin(scale, false, false);
                    currentHeight = (float) current.getHeight(false);
                }
                float textY = baseY + (lineHeight - currentHeight) * 0.5f;
                float gx = g.x0() + xOffset;
                int shadowColor = BetterChatDrawUtil.mulAlpha(0xFF000000, alpha * shadowAlpha);
                BetterChatDrawUtil.applyColor(shadowColor);
                current.render(g.text(), gx + shadowOffset, textY, BetterChatRenderer.TMP_COLOR, false);
                current.render(g.text(), gx - shadowOffset, textY, BetterChatRenderer.TMP_COLOR, false);
                current.render(g.text(), gx, textY + shadowOffset, BetterChatRenderer.TMP_COLOR, false);
                current.render(g.text(), gx, textY - shadowOffset, BetterChatRenderer.TMP_COLOR, false);
                BetterChatDrawUtil.applyColor(BetterChatDrawUtil.mulAlpha(g.color(), alpha));
                current.render(g.text(), gx, textY, BetterChatRenderer.TMP_COLOR, false);
            }
        }
        if (current != null) {
            current.end();
        }
    }

    private static void renderCommandPrefixRun(List<BetterChatModel.GlyphBox> glyphs,
                                               int from, int to,
                                               float baseY, float lineHeight, float xOffset,
                                               float scale, float alpha, float shadowOffset, float shadowAlpha,
                                               Component source) {
        if (from < 0 || to <= from || to > glyphs.size()) return;
        BetterChatModel.GlyphBox first = glyphs.get(from);
        BetterChatModel.GlyphBox last = glyphs.get(to - 1);
        TextRenderer tr = BetterChatFontUtil.fontRenderer(first.font());
        if (tr == null) return;

        StringBuilder text = new StringBuilder();
        for (int i = from; i < to; i++) {
            text.append(glyphs.get(i).text());
        }
        if (text.isEmpty()) return;

        float runX = first.x0() + xOffset;
        float runW = Math.max(1.0f, last.x1() - first.x0());
        CommandOutput.Tone tone = CommandOutput.toneOf(source);
        int start = CommandOutput.prefixStartColor(tone);
        int end = CommandOutput.prefixEndColor(tone);
        int bottomMix = CommandOutput.mixRgb(end, 0xFFFFFFFF, 0.10f);

        tr.begin(scale, false, false);
        try {
            float textY = baseY + (lineHeight - (float) tr.getHeight(false)) * 0.5f;
            int shadowColor = BetterChatDrawUtil.mulAlpha(0xFF000000, alpha * shadowAlpha);
            BetterChatDrawUtil.applyColor(shadowColor);
            tr.render(text.toString(), runX + shadowOffset, textY, BetterChatRenderer.TMP_COLOR, false);
            tr.render(text.toString(), runX - shadowOffset, textY, BetterChatRenderer.TMP_COLOR, false);
            tr.render(text.toString(), runX, textY + shadowOffset, BetterChatRenderer.TMP_COLOR, false);
            tr.render(text.toString(), runX, textY - shadowOffset, BetterChatRenderer.TMP_COLOR, false);

            long now = GameClock.millis();
            float flow = (now % 2600L) / 2600.0f;
            float breath = 0.5f + 0.5f * (float) Math.sin(now / 5200.0 * Math.PI * 2.0);
            tr.renderQuadGradient(text.toString(), runX, textY, (idx, cp, x0, y0, x1, y1, out) -> {
                float t0 = (float) ((x0 - runX) / runW) + flow;
                float t1 = (float) ((x1 - runX) / runW) + flow;
                if (t0 > 1.0f) t0 -= 1.0f;
                if (t1 > 1.0f) t1 -= 1.0f;
                int base0 = CommandOutput.mixRgb(start, end, t0);
                int base1 = CommandOutput.mixRgb(start, end, t1);
                int leftTop = BetterChatDrawUtil.mulAlpha(mixBreath(base0, breath), alpha);
                int rightTop = BetterChatDrawUtil.mulAlpha(mixBreath(base1, breath), alpha);
                int leftBottom = BetterChatDrawUtil.mulAlpha(mixBreath(CommandOutput.mixRgb(start, bottomMix, t0), breath), alpha);
                int rightBottom = BetterChatDrawUtil.mulAlpha(mixBreath(CommandOutput.mixRgb(start, bottomMix, t1), breath), alpha);
                out[0] = leftTop;
                out[1] = leftBottom;
                out[2] = rightBottom;
                out[3] = rightTop;
            }, false);
        } finally {
            tr.end();
        }
    }

    private static int mixBreath(int rgb, float breath) {
        return CommandOutput.mixRgb(rgb, 0xFFFFFF, 0.06f * breath);
    }
}
