package dev.loki.lovisual.features.theme;

import com.google.gson.JsonObject;
import dev.loki.lovisual.render.engine.renderer.renderer2d.fx.backdrop.ThemeBackdrop2D;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class BackdropTest {
    @Test
    void valuesAreClampedAndNoneIsInactive() {
        Backdrop wild = new Backdrop(Backdrop.Mode.MAGMA, 9.0f, 0.0f, 100.0f);
        assertEquals(Backdrop.MAX_INTENSITY, wild.intensity());
        assertEquals(Backdrop.MIN_SPEED, wild.speed());
        assertEquals(Backdrop.MAX_SCALE, wild.scale());
        assertFalse(Backdrop.NONE.isActive());
        assertFalse(Backdrop.of(Backdrop.Mode.MAGMA, 0.0f).isActive());
        assertTrue(Backdrop.of(Backdrop.Mode.MAGMA, 0.5f).isActive());
    }

    @Test
    void sameModeBlendsAndDifferentModeFadesTheNewOneIn() {
        Backdrop a = new Backdrop(Backdrop.Mode.EMBER, 1.0f, 1.0f, 1.0f);
        Backdrop b = new Backdrop(Backdrop.Mode.EMBER, 0.0f, 2.0f, 1.0f);
        assertEquals(0.5f, a.lerp(b, 0.5f).intensity(), 1e-6f);
        assertEquals(1.5f, a.lerp(b, 0.5f).speed(), 1e-6f);
        Backdrop other = Backdrop.of(Backdrop.Mode.GRID_HORIZON, 0.8f);
        assertEquals(Backdrop.Mode.GRID_HORIZON, a.lerp(other, 0.25f).mode());
        assertEquals(0.2f, a.lerp(other, 0.25f).intensity(), 1e-6f);
    }

    @Test
    void modesParseByIdAndFallBackToNone() {
        assertEquals(Backdrop.Mode.DIGITAL_RAIN, Backdrop.Mode.byId(" Digital_Rain "));
        assertEquals(Backdrop.Mode.NONE, Backdrop.Mode.byId("nope"));
        assertEquals(Backdrop.Mode.NONE, Backdrop.Mode.byId(null));
        assertEquals(6, Backdrop.Mode.EMBER.shaderIndex());
    }

    @Test
    void jsonRoundTripKeepsTheBackdropAndOldFilesReadAsNone() {
        Themes.ThemeEntry base = Themes.presetEntry(Themes.THEME_CLASSIC);
        Backdrop backdrop = new Backdrop(Backdrop.Mode.SINGULARITY, 0.9f, 1.5f, 2.0f);
        JsonObject json = ThemeStoreJson.entryToJson(new Themes.ThemeEntry("my_theme", "My", false, base.theme(),
                base.windowGradient(), base.headerGradient(), base.surfaceGradient(), base.cardGradient(),
                base.strokeGradient(), backdrop));
        Themes.ThemeEntry read = ThemeStoreJson.parseEntry(json, false);
        assertEquals(backdrop, read.backdrop());
        json.remove("backdrop");
        assertEquals(Backdrop.NONE, ThemeStoreJson.parseEntry(json, false).backdrop());
    }

    @Test
    void profileMapRoundTripKeepsTheBackdrop() {
        Themes.ThemeEntry base = Themes.presetEntry(Themes.THEME_CINDER);
        assertEquals(Backdrop.Mode.MAGMA, base.backdrop().mode());
        Map<String, Object> values = Themes.toProfileValues(base);
        Themes.ThemeEntry read = Themes.fromProfileValues("zz_cinder_copy", "Copy", values);
        assertEquals(base.backdrop(), read.backdrop());
    }

    @Test
    void sixPresetsShipWithABackdropAndEntriesDefaultToNone() {
        long withBackdrop = Themes.presetEntries().stream().filter(e -> e.backdrop().isActive()).count();
        assertEquals(6, withBackdrop);
        Themes.ThemeEntry plain = Themes.presetEntry(Themes.THEME_CLASSIC);
        assertEquals(Backdrop.NONE, plain.backdrop());
        assertEquals(Backdrop.Mode.EMBER, Themes.presetEntry(Themes.THEME_AMBER).backdrop().mode());
    }

    @Test
    void paletteRampsFromDarkToLight() {
        int[] palette = ThemeBackdrop2D.palette(0xFFE69A43, 0xFFFFFFFF);
        assertEquals(3, palette.length);
        assertTrue((palette[0] >> 16 & 0xFF) < (palette[1] >> 16 & 0xFF));
        assertTrue((palette[2] >> 8 & 0xFF) > (palette[1] >> 8 & 0xFF));
        assertEquals(0xFF, palette[1] >>> 24);
    }
}
