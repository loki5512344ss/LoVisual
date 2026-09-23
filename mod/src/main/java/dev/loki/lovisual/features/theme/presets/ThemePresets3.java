package dev.loki.lovisual.features.theme;

import dev.loki.lovisual.features.theme.Themes.Theme;
import dev.loki.lovisual.features.theme.Themes.ThemeEntry;
import dev.loki.lovisual.features.theme.Themes.GradientSpec;

/**
 * Пресет тем 3: LILAC, SAND, DUSK, PRISM, CINDER, FOREST.
 */
final class ThemePresets3 {
    private ThemePresets3() {}

static final Theme LILAC = new Theme(
            0xFF14131E, // window bg
            0xFF1A1728, // header
            0x60433A5A, // stroke
            0xFF1B172A, // surface
            0xFF241D36, // hover
            0x506C5CBE, // card enabled overlay
            0x40312644, // card disabled
            0xFFF5F2FF, // text primary
            0x88CFC6E6, // text muted
            0xFFB79BFF, // accent
            0x80C8B0FF, // accent soft
            0x60705A9C  // stroke soft
    );
static final Theme SAND = new Theme(
            0xFF181510, // window bg
            0xFF201A13, // header
            0x6050432F, // stroke
            0xFF221B14, // surface
            0xFF2C231B, // hover
            0x50E8C98C, // card enabled overlay
            0x40322A21, // card disabled
            0xFFFFF4E8, // text primary
            0x88D8C4A2, // text muted
            0xFFE8C98C, // accent
            0x80F0D8A8, // accent soft
            0x607A6A54  // stroke soft
    );
static final Theme DUSK = new Theme(
            0xFF141018, // window bg
            0xFF1B1422, // header
            0x60503A4A, // stroke
            0xFF1D1626, // surface
            0xFF271B34, // hover
            0x50FF875A, // card enabled overlay
            0x4030263B, // card disabled
            0xFFF6F1FF, // text primary
            0x88D6C9E6, // text muted
            0xFFFF8A4C, // accent
            0x80FF9B5A, // accent soft
            0x606B5A7A  // stroke soft
    );
static final Theme PRISM = new Theme(
            0xFF0F1220, // window bg
            0xFF141A2B, // header
            0x60435173, // stroke
            0xFF151C2E, // surface
            0xFF1B243A, // hover
            0x504B7CFF, // card enabled overlay
            0x4031324A, // card disabled
            0xFFF3F6FF, // text primary
            0x88C2CDEB, // text muted
            0xFF5AC8FA, // accent
            0x8074B6FF, // accent soft
            0x606B7A9E  // stroke soft
    );
static final Theme CINDER = new Theme(
            0xFF120E0F, // window bg
            0xFF181012, // header
            0x604A2F32, // stroke
            0xFF1A1214, // surface
            0xFF23171A, // hover
            0x50D24A4A, // card enabled overlay
            0x40281A1C, // card disabled
            0xFFFFF3F3, // text primary
            0x88E3B9B9, // text muted
            0xFFDC4A4A, // accent
            0x80E06A6A, // accent soft
            0x606B4E52  // stroke soft
    );
static final Theme FOREST = new Theme(
            0xFF101611, // window bg
            0xFF141E15, // header
            0x6043573D, // stroke
            0xFF161F18, // surface
            0xFF1D2B20, // hover
            0x5071C36C, // card enabled overlay
            0x40263225, // card disabled
            0xFFF3FFF1, // text primary
            0x88C5E3C1, // text muted
            0xFF7DD36B, // accent
            0x807DD36B, // accent soft
            0x60607B58  // stroke soft
    );
static final ThemeEntry LILAC_ENTRY = new ThemeEntry(
            Themes.THEME_LILAC,
            "Lilac",
            true,
            LILAC,
            new GradientSpec(true, 0xFF241A3B, 0xFF1A3A55, 120f),
            new GradientSpec(true, 0xFF2F214A, 0xFF3D2F6A, 120f),
            new GradientSpec(true, 0xFF1B172A, 0xFF15121F, 90f),
            new GradientSpec(true, 0xFFB79BFF, 0xFF7AD9FF, 45f),
            new GradientSpec(true, 0xFFC5B0FF, 0xFF8BB2FF, 120f)
    );
static final ThemeEntry SAND_ENTRY = new ThemeEntry(
            Themes.THEME_SAND,
            "Sand",
            true,
            SAND,
            new GradientSpec(true, 0xFF2A2016, 0xFF3A2F22, 45f),
            new GradientSpec(true, 0xFF32261B, 0xFF4A3A2B, 45f),
            new GradientSpec(true, 0xFF221B14, 0xFF1A1510, 90f),
            new GradientSpec(true, 0xFFF2D29B, 0xFFE8B67A, 45f),
            new GradientSpec(true, 0xFFF5E0B2, 0xFFD1A36C, 45f)
    );
static final ThemeEntry DUSK_ENTRY = new ThemeEntry(
            Themes.THEME_DUSK,
            "Dusk",
            true,
            DUSK,
            new GradientSpec(true, 0xFF2B1630, 0xFF6B2A1F, 45f),
            new GradientSpec(true, 0xFF3B1B3A, 0xFF8A3B24, 45f),
            new GradientSpec(true, 0xFF1D1626, 0xFF141018, 90f),
            new GradientSpec(true, 0xFF7C4BFF, 0xFFFF9B3D, 45f),
            new GradientSpec(true, 0xFFA775FF, 0xFFFF7A3A, 45f)
    );
static final ThemeEntry PRISM_ENTRY = new ThemeEntry(
            Themes.THEME_PRISM,
            "Prism",
            true,
            PRISM,
            new GradientSpec(true, 0xFF1A243A, 0xFF2A1E4A, 135f),
            new GradientSpec(true, 0xFF223050, 0xFF352060, 135f),
            new GradientSpec(true, 0xFF151C2E, 0xFF101522, 90f),
            new GradientSpec(true, 0xFF59C4FF, 0xFFB35CFF, 45f),
            new GradientSpec(true, 0xFF7AD4FF, 0xFFE36BFF, 45f)
    );
static final ThemeEntry CINDER_ENTRY = new ThemeEntry(
            Themes.THEME_CINDER,
            "Cinder",
            true,
            CINDER,
            new GradientSpec(true, 0xFF1A0C0C, 0xFF2A1416, 45f),
            new GradientSpec(true, 0xFF241012, 0xFF3A191C, 45f),
            new GradientSpec(true, 0xFF1A1214, 0xFF120E0F, 90f),
            new GradientSpec(true, 0xFFDC4A4A, 0xFF401010, 45f),
            new GradientSpec(true, 0xFFE26A6A, 0xFF7A2A2A, 45f)
    );
static final ThemeEntry FOREST_ENTRY = new ThemeEntry(
            Themes.THEME_FOREST,
            "Forest",
            true,
            FOREST,
            new GradientSpec(true, 0xFF1A241C, 0xFF2B3A24, 120f),
            new GradientSpec(true, 0xFF1F2B21, 0xFF2F4B2E, 120f),
            new GradientSpec(true, 0xFF161F18, 0xFF101611, 90f),
            new GradientSpec(true, 0xFF7DD36B, 0xFFC6E886, 45f),
            new GradientSpec(true, 0xFF9BE07C, 0xFF6BC26A, 45f)
    );
}
