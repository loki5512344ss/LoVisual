package dev.loki.lovisual.features.gui.hud.nondraggable.impl.tab;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Word / grapheme-aware line splitter for tab-list header and footer components.
 * Preserves per-fragment {@link Style}s so rich text survives the wrap; falls back
 * to grapheme-level splitting when a single token exceeds the vanilla font width.
 * Extracted from {@link TabListModel} so the model class stays within the file cap.
 */
final class TabListTextSplitter {

    private TabListTextSplitter() { }

    /** Flatten a component to styled fragments then wrap into lines that fit {@code maxVanillaWidth}. */
    static List<Component> split(Minecraft mc, Component component, int maxVanillaWidth) {
        if (component == null || component.getString().isBlank()) return List.of();
        if (mc == null || mc.font == null || maxVanillaWidth <= 0) return List.of(component);
        List<StyledFragment> fragments = flatten(component);
        if (fragments.isEmpty()) return List.of(component);
        List<Component> lines = new ArrayList<>();
        MutableComponentBuilder current = new MutableComponentBuilder();
        for (StyledFragment fragment : fragments) appendStyledFragment(mc, lines, current, fragment, maxVanillaWidth);
        flushStyledLine(lines, current);
        if (lines.isEmpty()) return List.of(component);
        if (lines.size() == 1 && lines.get(0).getString().equals(component.getString())) return List.of(component);
        return List.copyOf(lines);
    }

    private static List<StyledFragment> flatten(Component component) {
        List<StyledFragment> out = new ArrayList<>();
        component.visit((style, text) -> {
            if (text != null && !text.isEmpty()) out.add(new StyledFragment(text, style == null ? Style.EMPTY : style));
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    private static void appendStyledFragment(Minecraft mc, List<Component> out, MutableComponentBuilder current, StyledFragment fragment, int maxVanillaWidth) {
        String text = fragment.text();
        Style style = fragment.style();
        StringBuilder token = new StringBuilder();
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (cp == '\r') {
                if (i < text.length() && text.charAt(i) == '\n') i++;
                appendStyledToken(mc, out, current, token.toString(), style, maxVanillaWidth);
                token.setLength(0);
                flushStyledLine(out, current);
                continue;
            }
            if (cp == '\n') {
                appendStyledToken(mc, out, current, token.toString(), style, maxVanillaWidth);
                token.setLength(0);
                flushStyledLine(out, current);
                continue;
            }
            boolean whitespace = Character.isWhitespace(cp);
            if (whitespace && token.length() > 0 && !isWhitespaceToken(token)) {
                appendStyledToken(mc, out, current, token.toString(), style, maxVanillaWidth);
                token.setLength(0);
            } else if (!whitespace && token.length() > 0 && isWhitespaceToken(token)) {
                appendStyledToken(mc, out, current, token.toString(), style, maxVanillaWidth);
                token.setLength(0);
            }
            token.appendCodePoint(cp);
        }
        appendStyledToken(mc, out, current, token.toString(), style, maxVanillaWidth);
    }

    private static boolean isWhitespaceToken(StringBuilder token) {
        if (token == null || token.length() == 0) return false;
        for (int i = 0; i < token.length(); ) {
            int cp = token.codePointAt(i);
            if (!Character.isWhitespace(cp)) return false;
            i += Character.charCount(cp);
        }
        return true;
    }

    private static void appendStyledToken(Minecraft mc, List<Component> out, MutableComponentBuilder current, String token, Style style, int maxVanillaWidth) {
        if (token == null || token.isEmpty()) return;
        if (current.isEmpty() && token.isBlank()) return;
        Component tokenComponent = Component.literal(token).withStyle(style);
        int tokenWidth = mc.font.width(tokenComponent);
        if (!current.isEmpty() && current.width + tokenWidth > maxVanillaWidth) {
            flushStyledLine(out, current);
            if (token.isBlank()) return;
        }
        if (tokenWidth <= maxVanillaWidth) {
            current.append(tokenComponent, tokenWidth);
            return;
        }
        for (int i = 0; i < token.length(); ) {
            int cp = token.codePointAt(i);
            String glyph = new String(Character.toChars(cp));
            Component glyphComponent = Component.literal(glyph).withStyle(style);
            int glyphWidth = mc.font.width(glyphComponent);
            if (!current.isEmpty() && current.width + glyphWidth > maxVanillaWidth) flushStyledLine(out, current);
            current.append(glyphComponent, glyphWidth);
            i += Character.charCount(cp);
        }
    }

    private static void flushStyledLine(List<Component> out, MutableComponentBuilder current) {
        if (current == null || current.isEmpty()) return;
        out.add(current.build());
        current.clear();
    }

    /** Text + style pair produced by {@link Component#visit}. */
    record StyledFragment(String text, Style style) { }

    /** Accumulates {@link Component}s into one line and tracks the running vanilla pixel width. */
    static final class MutableComponentBuilder {
        private MutableComponent line = Component.empty();
        private int width;
        private boolean empty = true;
        boolean isEmpty() { return empty; }
        void append(Component component, int componentWidth) {
            if (component == null) return;
            line.append(component);
            width += Math.max(0, componentWidth);
            empty = false;
        }
        Component build() { return line; }
        void clear() { line = Component.empty(); width = 0; empty = true; }
    }
}
