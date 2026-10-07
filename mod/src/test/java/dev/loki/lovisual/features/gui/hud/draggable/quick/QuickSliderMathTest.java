package dev.loki.lovisual.features.gui.hud.draggable.quick;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class QuickSliderMathTest {

    @Test
    void valueFollowsTheCursorAcrossTheTrack() {
        assertEquals(0.0, QuickSliderMath.valueAt(10f, 10f, 100f, 0, 10, false), 1e-9);
        assertEquals(5.0, QuickSliderMath.valueAt(60f, 10f, 100f, 0, 10, false), 1e-9);
        assertEquals(10.0, QuickSliderMath.valueAt(110f, 10f, 100f, 0, 10, false), 1e-9);
    }

    @Test
    void cursorBeyondTheTrackIsClamped() {
        assertEquals(1.0, QuickSliderMath.valueAt(-50f, 10f, 100f, 1, 4, true), 1e-9);
        assertEquals(4.0, QuickSliderMath.valueAt(500f, 10f, 100f, 1, 4, true), 1e-9);
    }

    @Test
    void integerSettingsRoundToWholeNumbersAndFloatsToHundredths() {
        assertEquals(3.0, QuickSliderMath.valueAt(60f, 10f, 100f, 1, 4, true), 1e-9);
        assertEquals(0.33, QuickSliderMath.valueAt(43f, 10f, 100f, 0, 1, false), 1e-9);
    }

    @Test
    void ratioIsClampedAndSafeForEmptyRanges() {
        assertEquals(0.5, QuickSliderMath.ratio(5, 0, 10), 1e-9);
        assertEquals(1.0, QuickSliderMath.ratio(20, 0, 10), 1e-9);
        assertEquals(0.0, QuickSliderMath.ratio(3, 5, 5), 1e-9);
    }
}
