package dev.loki.lovisual.features.theme;

import dev.loki.lovisual.features.theme.Themes.Theme;
import dev.loki.lovisual.features.theme.Themes.ThemeEntry;
import dev.loki.lovisual.features.theme.Themes.GradientSpec;

/**
 * Пресет тем 2: AZURE, NITRO, SUNSET, AURORA, MINT, PEACH.
 */
final class ThemePresets2 {
    private ThemePresets2() {}

static final Theme AZURE = new Theme(
            0xF00C1220, // window bg (blue)
            0xF0101A2A, // header
            0x60324B66, // stroke
            0xFF121C2C, // surface
            0xFF18253A, // hover
            0x503F6BAA, // card enabled overlay
            0x40283348, // card disabled
            0xFFF2F7FF, // text primary
            0x88BBD0EA, // text muted
            0xFF5DA7FF, // accent
            0x805DA7FF, // accent soft
            0x60507CA8  // stroke soft
    );
static final Theme NITRO = new Theme(
            0xFF121624, // window bg
            0xFF181E33, // header
            0x60465580, // stroke
            0xFF1A1F36, // surface
            0xFF232A45, // hover
            0x50A24CFF, // card enabled overlay
            0x40303A52, // card disabled
            0xFFF3F6FF, // text primary
            0x88C7D3F3, // text muted
            0xFF7A5CFF, // accent
            0x807A5CFF, // accent soft
            0x60829AC9  // stroke soft
    );
static final Theme SUNSET = new Theme(
            0xFF1A1116, // window bg
            0xFF211319, // header
            0x605C2B2B, // stroke
            0xFF24151C, // surface
            0xFF2E1A23, // hover
            0x50FF7A45, // card enabled overlay
            0x40261A1A, // card disabled
            0xFFFFF2E8, // text primary
            0x88E3B9A3, // text muted
            0xFFFF7A45, // accent
            0x80FF7A45, // accent soft
            0x60F5C1A6  // stroke soft
    );
static final Theme AURORA = new Theme(
            0xFF0D1A18, // window bg
            0xFF102320, // header
            0x602B5B57, // stroke
            0xFF132623, // surface
            0xFF1A312D, // hover
            0x5034D399, // card enabled overlay
            0x40304742, // card disabled
            0xFFF1FFF9, // text primary
            0x8897E3D0, // text muted
            0xFF2DD9C3, // accent
            0x802DD9C3, // accent soft
            0x6071B5A6  // stroke soft
    );
static final Theme MINT = new Theme(
            0xFF0E1917, // window bg
            0xFF112320, // header
            0x60255B53, // stroke
            0xFF132521, // surface
            0xFF1A2F2B, // hover
            0x5035CFA8, // card enabled overlay
            0x40304742, // card disabled
            0xFFF1FFF9, // text primary
            0x8897E3D0, // text muted
            0xFF43E0B2, // accent
            0x8043E0B2, // accent soft
            0x6071B5A6  // stroke soft
    );
static final Theme PEACH = new Theme(
            0xFF1A1210, // window bg
            0xFF221510, // header
            0x605C3A2A, // stroke
            0xFF241814, // surface
            0xFF2F211A, // hover
            0x50F2A269, // card enabled overlay
            0x40382A22, // card disabled
            0xFFFFF7F0, // text primary
            0x88E3C9A7, // text muted
            0xFFFFA46B, // accent
            0x80FFB680, // accent soft
            0x60F5D7B0  // stroke soft
    );
static final ThemeEntry AZURE_ENTRY = new ThemeEntry(
            Themes.THEME_AZURE,
            "Azure",
            true,
            AZURE,
            Themes.flatGradient(AZURE.windowBg()),
            Themes.subtleHeaderGradient(AZURE.windowHeader()),
            Themes.flatGradient(AZURE.surface()),
            Themes.flatGradient(AZURE.cardEnabled()),
            Themes.flatGradient(AZURE.windowStroke())
    );
static final ThemeEntry NITRO_ENTRY = new ThemeEntry(
            Themes.THEME_NITRO,
            "Nitro",
            true,
            NITRO,
            new GradientSpec(true, 0xFF36205F, 0xFF0B7DFF, 45f),
            new GradientSpec(true, 0xFF4B2FAF, 0xFF24C1FF, 45f),
            new GradientSpec(true, 0xFF1A1F36, 0xFF121728, 90f),
            new GradientSpec(true, 0xFF5A37D5, 0xFF2AD1FF, 45f),
            new GradientSpec(true, 0xFF6C4BFF, 0xFF4BD0FF, 45f)
    );
static final ThemeEntry SUNSET_ENTRY = new ThemeEntry(
            Themes.THEME_SUNSET,
            "Sunset",
            true,
            SUNSET,
            new GradientSpec(true, 0xFF2A1020, 0xFFB2431F, 45f),
            new GradientSpec(true, 0xFF3B1626, 0xFFC25B2A, 45f),
            new GradientSpec(true, 0xFF24151C, 0xFF1E1116, 90f),
            new GradientSpec(true, 0xFFFF8A4C, 0xFFFF4B7A, 45f),
            new GradientSpec(true, 0xFFFFB27A, 0xFFFF6A3A, 45f)
    );
static final ThemeEntry AURORA_ENTRY = new ThemeEntry(
            Themes.THEME_AURORA,
            "Aurora",
            true,
            AURORA,
            new GradientSpec(true, 0xFF0B2A3C, 0xFF2A8E6B, 120f),
            new GradientSpec(true, 0xFF0E3B46, 0xFF1FB88B, 120f),
            new GradientSpec(true, 0xFF132623, 0xFF10201D, 90f),
            new GradientSpec(true, 0xFF2DD9C3, 0xFF5A7CFF, 135f),
            new GradientSpec(true, 0xFF78FFE5, 0xFF6FB1FF, 120f)
    );
static final ThemeEntry MINT_ENTRY = new ThemeEntry(
            Themes.THEME_MINT,
            "Mint",
            true,
            MINT,
            new GradientSpec(true, 0xFF0B2A24, 0xFF1B3E36, 120f),
            new GradientSpec(true, 0xFF0F3A32, 0xFF1A6E59, 120f),
            new GradientSpec(true, 0xFF132521, 0xFF0F1E1B, 90f),
            new GradientSpec(true, 0xFF3DE3B4, 0xFFB7F2D9, 45f),
            new GradientSpec(true, 0xFF48E6B9, 0xFF7EF0D6, 120f)
    );
static final ThemeEntry PEACH_ENTRY = new ThemeEntry(
            Themes.THEME_PEACH,
            "Peach",
            true,
            PEACH,
            new GradientSpec(true, 0xFF2A1512, 0xFF4B241A, 45f),
            new GradientSpec(true, 0xFF3B1B14, 0xFF6A301F, 45f),
            new GradientSpec(true, 0xFF241814, 0xFF1C1310, 90f),
            new GradientSpec(true, 0xFFFFB47A, 0xFFFF7FA8, 45f),
            new GradientSpec(true, 0xFFFFC28E, 0xFFFF8A5A, 45f)
    );
}
