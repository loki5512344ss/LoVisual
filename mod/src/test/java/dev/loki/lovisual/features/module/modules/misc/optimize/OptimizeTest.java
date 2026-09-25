package dev.loki.lovisual.features.module.modules.misc.optimize;

import dev.loki.lovisual.render.engine.optimize.OptimizeState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class OptimizeTest {

    @Test
    void stateIsCleanUntilApplied() {
        OptimizeState.reset();
        assertFalse(OptimizeState.isNoGlass());
        assertFalse(OptimizeState.isLitePost());
        assertFalse(OptimizeState.isLiteSky());
        assertFalse(OptimizeState.isLeanHud());
    }

    @Test
    void applyTracksEachKnobIndependently() {
        OptimizeState.apply(true, false, true, false);
        assertTrue(OptimizeState.isNoGlass());
        assertFalse(OptimizeState.isLitePost());
        assertTrue(OptimizeState.isLiteSky());
        assertFalse(OptimizeState.isLeanHud());

        OptimizeState.reset();
        assertFalse(OptimizeState.isNoGlass());
        assertFalse(OptimizeState.isLiteSky());
    }

    @Test
    void summaryListsOnlyActiveKnobsInOrder() {
        assertEquals("no knobs active", Optimize.summaryOf(false, false, false, false));
        assertEquals("no glass, light post", Optimize.summaryOf(true, true, false, false));
        assertEquals("simple sky, lean HUD", Optimize.summaryOf(false, false, true, true));
        assertEquals(
                "no glass, light post, simple sky, lean HUD",
                Optimize.summaryOf(true, true, true, true));
    }
}
