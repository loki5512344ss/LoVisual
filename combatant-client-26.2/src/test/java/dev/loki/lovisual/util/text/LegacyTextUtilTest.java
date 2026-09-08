/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.text;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * convertLegacyCodes строит дерево: Component.empty() → сегмент-обёртка → стилизованные листья.
 * firstLeaf() спускается к первому листу с текстом.
 */
final class LegacyTextUtilTest {

    private static Component firstLeaf(Component in) {
        Component cur = in;
        while (!cur.getSiblings().isEmpty()) {
            cur = cur.getSiblings().get(0);
        }
        return cur;
    }

    /** Все листовые компоненты с непустым текстом, в порядке обхода. */
    private static java.util.List<Component> leaves(Component in) {
        java.util.List<Component> out = new java.util.ArrayList<>();
        collectLeaves(in, out);
        return out;
    }

    private static void collectLeaves(Component in, java.util.List<Component> out) {
        if (in.getSiblings().isEmpty()) {
            if (!in.getString().isEmpty()) out.add(in);
            return;
        }
        for (Component child : in.getSiblings()) {
            collectLeaves(child, out);
        }
    }

    @Test
    void ampersandToSectionConvertsColorCodes() {
        assertEquals("§aGreen", LegacyTextUtil.ampersandToSection("&aGreen"));
        assertEquals("plain text", LegacyTextUtil.ampersandToSection("plain text"));
    }

    @Test
    void ampersandToSectionConvertsHexNotation() {
        assertEquals("§x§F§F§0§0§0§0X", LegacyTextUtil.ampersandToSection("&#FF0000X"));
        assertEquals("§x§0§1§2§3§4§5", LegacyTextUtil.ampersandToSection("&x&0&1&2&3&4&5"));
    }

    @Test
    void ampersandToSectionKeepsLoneAmpersands() {
        assertEquals("at & end", LegacyTextUtil.ampersandToSection("at & end"));
    }

    @Test
    void ampersandToSectionHandlesNullAndEmpty() {
        assertNull(LegacyTextUtil.ampersandToSection(null));
        assertEquals("", LegacyTextUtil.ampersandToSection(""));
    }

    @Test
    void stripLegacyRemovesColorAndDecorationCodes() {
        assertEquals("Green", LegacyTextUtil.stripLegacy("§aGreen"));
        assertEquals("RedBold", LegacyTextUtil.stripLegacy("&cRed§lBold"));
        assertEquals("Hex", LegacyTextUtil.stripLegacy("§x§F§F§0§0§0§0Hex"));
        assertEquals("", LegacyTextUtil.stripLegacy(null));
        assertEquals("no codes", LegacyTextUtil.stripLegacy("no codes"));
    }

    @Test
    void convertLegacyCodesReturnsSameInstanceWithoutCodes() {
        Component in = Component.literal("plain");
        assertSame(in, LegacyTextUtil.convertLegacyCodes(in));
    }

    @Test
    void convertLegacyCodesHandlesNull() {
        assertEquals("", LegacyTextUtil.convertLegacyCodes(null).getString());
    }

    @Test
    void convertLegacyCodesAppliesVanillaColor() {
        Component out = LegacyTextUtil.convertLegacyCodes(Component.literal("&aHi"));

        assertEquals("Hi", out.getString());
        assertEquals(TextColor.fromRgb(0x55FF55), firstLeaf(out).getStyle().getColor());
    }

    @Test
    void convertLegacyCodesAppliesHexColor() {
        Component out = LegacyTextUtil.convertLegacyCodes(Component.literal("§x§F§F§0§0§0§0Hex"));

        assertEquals("Hex", out.getString());
        assertEquals(TextColor.fromRgb(0xFF0000), firstLeaf(out).getStyle().getColor());
    }

    @Test
    void convertLegacyCodesAppliesBedrockMinecoinGold() {
        Component out = LegacyTextUtil.convertLegacyCodes(Component.literal("§gCoin"));

        assertEquals("Coin", out.getString());
        assertEquals(TextColor.fromRgb(0xDDD605), firstLeaf(out).getStyle().getColor());
    }

    @Test
    void convertLegacyCodesKeepsBaseStyle() {
        Style base = Style.EMPTY.withBold(true);
        Component in = Component.literal("§aHi").setStyle(base);

        Component out = LegacyTextUtil.convertLegacyCodes(in);
        Style leafStyle = firstLeaf(out).getStyle();

        assertEquals(TextColor.fromRgb(0x55FF55), leafStyle.getColor());
        assertEquals(Boolean.TRUE, leafStyle.isBold());
    }

    @Test
    void convertLegacyCodesSplitsMultiStyleSegments() {
        Component out = LegacyTextUtil.convertLegacyCodes(Component.literal("&aRed&lBold"));

        assertEquals("RedBold", out.getString());
    }

    @Test
    void colorCodeResetsDecorationsToBase() {
        // парсер трактует цвет по-ваналлийски: код цвета возвращает стиль к base + цвет,
        // более ранние decoration-коды из строки сбрасываются (селектор isColor должен их распознавать)
        Component out = LegacyTextUtil.convertLegacyCodes(Component.literal("§lBold§aGreen"));

        assertEquals("BoldGreen", out.getString());
        var leafList = leaves(out);
        assertEquals(2, leafList.size());

        Style boldStyle = leafList.get(0).getStyle();
        assertEquals(Boolean.TRUE, boldStyle.isBold());

        Style greenStyle = leafList.get(1).getStyle();
        assertEquals(TextColor.fromRgb(0x55FF55), greenStyle.getColor());
        assertEquals(Boolean.FALSE, greenStyle.isBold());
    }

    @Test
    void decorationCodeKeepsColorAndAddsFlag() {
        Component out = LegacyTextUtil.convertLegacyCodes(Component.literal("&aRed&lBold"));

        var leafList = leaves(out);
        assertEquals(2, leafList.size());
        Style boldStyle = leafList.get(1).getStyle();
        assertEquals(Boolean.TRUE, boldStyle.isBold());
        assertEquals(TextColor.fromRgb(0x55FF55), boldStyle.getColor());
    }
}
