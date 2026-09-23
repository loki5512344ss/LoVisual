package dev.loki.lovisual.features.theme;

import dev.loki.lovisual.features.theme.Themes.Theme;
import dev.loki.lovisual.features.theme.Themes.ThemeEntry;
import dev.loki.lovisual.features.theme.Themes.GradientSpec;

/**
 * Пресет тем 1: CLASSIC, LEGACY, AMBER, NOIR, PURPLE, BLUE.
 */
final class ThemePresets1 {
    private ThemePresets1() {}

static final Theme CLASSIC = new Theme(
            0xF012191B, // window bg (cooler, unchanged)
            0xF0142226, // header darker
            0x60203030, // window stroke
            0x16202523, // surface for controls
            0x28353A3E, // surface hover
            0x5043A7C8, // card enabled overlay
            0x334046,   // card disabled
            0xFFFFFFFF, // text primary
            0x88C6D2CD, // text muted
            0xFF5CC8E7, // accent
            0x805CC8E7, // accent soft
            0x60FFFFFF  // stroke soft
    );
static final Theme LEGACY = new Theme(
            0xF0121314, // window bg (old fixed style)
            0xF0000205, // header
            0xE1121314, // stroke
            0xFF171819, // surface
            0xFF131315, // surface hover
            0x503F464D, // card enabled overlay
            0x33191C20, // card disabled
            0xFFF5F5FF, // text primary
            0x88A5A5A5, // text muted
            0xFF767C94, // accent
            0x80767C94, // accent soft
            0x60373746  // stroke soft
    );
static final Theme AMBER = new Theme(
            0xF019120D, // window bg (warm)
            0xF0201711, // header
            0x60C3652C, // stroke
            0xFF221711, // surface
            0xFF2B1E16, // hover
            0x50E08C3A, // card enabled overlay
            0x40302620, // card disabled
            0xFFFFF7EB, // text primary
            0x88E3C9A7, // text muted
            0xFFE69A43, // accent
            0x80E69A43, // accent soft
            0x60F5D7B0  // stroke soft
    );
static final Theme NOIR = new Theme(
            0xF00C0D10, // window bg (deep charcoal)
            0xF0121317, // header
            0x60393F4A, // stroke
            0xFF14161B, // surface
            0xFF1C1F26, // hover
            0x5039424F, // card enabled overlay
            0x40282D35, // card disabled
            0xFFF4F4F4, // text primary
            0x88C7CBD3, // text muted
            0xFFE16B78, // accent
            0x80E16B78, // accent soft
            0x605A6370  // stroke soft
    );
static final Theme PURPLE = new Theme(
            0xF0120E1A, // window bg (purple)
            0xF0181323, // header
            0x60433A5A, // stroke
            0xFF1C1627, // surface
            0xFF261D35, // hover
            0x505E4AA0, // card enabled overlay
            0x40312644, // card disabled
            0xFFF5F2FF, // text primary
            0x88CFC6E6, // text muted
            0xFF9B6BFF, // accent
            0x809B6BFF, // accent soft
            0x60705A9C  // stroke soft
    );
static final Theme BLUE = new Theme(
            0xF0080F1E, // window bg (deep blue)
            0xF00D1626, // header
            0x60405A82, // stroke
            0xFF101B2B, // surface
            0xFF162338, // hover
            0x50406BB3, // card enabled overlay
            0x4023324A, // card disabled
            0xFFF0F5FF, // text primary
            0x88B2C4E6, // text muted
            0xFF2E64CC, // accent
            0x803A6FD6, // accent soft
            0x60465F8E  // stroke soft
    );
static final ThemeEntry CLASSIC_ENTRY = new ThemeEntry(
            Themes.THEME_CLASSIC,
            "Classic",
            true,
            CLASSIC,
            Themes.flatGradient(CLASSIC.windowBg()),
            Themes.subtleHeaderGradient(CLASSIC.windowHeader()),
            Themes.flatGradient(CLASSIC.surface()),
            Themes.flatGradient(CLASSIC.cardEnabled()),
            Themes.flatGradient(CLASSIC.windowStroke())
    );
static final ThemeEntry LEGACY_ENTRY = new ThemeEntry(
            Themes.THEME_LEGACY,
            "Legacy",
            true,
            LEGACY,
            Themes.flatGradient(LEGACY.windowBg()),
            Themes.subtleHeaderGradient(LEGACY.windowHeader()),
            Themes.flatGradient(LEGACY.surface()),
            Themes.flatGradient(LEGACY.cardEnabled()),
            Themes.flatGradient(LEGACY.windowStroke())
    );
static final ThemeEntry AMBER_ENTRY = new ThemeEntry(
            Themes.THEME_AMBER,
            "Amber",
            true,
            AMBER,
            Themes.flatGradient(AMBER.windowBg()),
            Themes.subtleHeaderGradient(AMBER.windowHeader()),
            Themes.flatGradient(AMBER.surface()),
            Themes.flatGradient(AMBER.cardEnabled()),
            Themes.flatGradient(AMBER.windowStroke())
    );
static final ThemeEntry NOIR_ENTRY = new ThemeEntry(
            Themes.THEME_NOIR,
            "Noir",
            true,
            NOIR,
            Themes.flatGradient(NOIR.windowBg()),
            Themes.subtleHeaderGradient(NOIR.windowHeader()),
            Themes.flatGradient(NOIR.surface()),
            Themes.flatGradient(NOIR.cardEnabled()),
            Themes.flatGradient(NOIR.windowStroke())
    );
static final ThemeEntry PURPLE_ENTRY = new ThemeEntry(
            Themes.THEME_PURPLE,
            "Purple",
            true,
            PURPLE,
            Themes.flatGradient(PURPLE.windowBg()),
            Themes.subtleHeaderGradient(PURPLE.windowHeader()),
            Themes.flatGradient(PURPLE.surface()),
            Themes.flatGradient(PURPLE.cardEnabled()),
            Themes.flatGradient(PURPLE.windowStroke())
    );
static final ThemeEntry BLUE_ENTRY = new ThemeEntry(
            Themes.THEME_BLUE,
            "Blue",
            true,
            BLUE,
            Themes.flatGradient(BLUE.windowBg()),
            Themes.subtleHeaderGradient(BLUE.windowHeader()),
            Themes.flatGradient(BLUE.surface()),
            Themes.flatGradient(BLUE.cardEnabled()),
            Themes.flatGradient(BLUE.windowStroke())
    );
}
