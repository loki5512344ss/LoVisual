package dev.loki.lovisual.features.module.modules.misc.optimize.text;

import dev.loki.lovisual.render.engine.optimize.OptimizeToggles;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class StableTextTest {

    /** A trivial FormattedCharSequence that just records whether accept() was called. */
    private static final class Recorder implements FormattedCharSequence {
        boolean accepted;

        @Override
        public boolean accept(FormattedCharSink sink) {
            accepted = true;
            return true;
        }
    }

    @AfterEach
    void resetTogglesAndTracked() {
        OptimizeToggles.reset();
        StableText.clearTracked();
    }

    @Test
    void markReturnsStableTextInstance() {
        OptimizeToggles.apply(
                false, 0,
                false, false, false,
                false, 0,
                false, false, false, false);
        Recorder inner = new Recorder();
        FormattedCharSequence marked = StableText.mark(inner);
        assertTrue(marked instanceof StableText,
                "mark() should return a StableText wrapper");
        assertEquals(1, StableText.trackedCount());
    }

    @Test
    void isStableReturnsTrueForMarkedText() {
        OptimizeToggles.apply(
                false, 0,
                false, false, false,
                false, 0,
                false, false, false, false);
        Recorder inner = new Recorder();
        FormattedCharSequence marked = StableText.mark(inner);
        assertTrue(StableText.isStable(marked));
    }

    @Test
    void isStableReturnsFalseForUnmarkedText() {
        OptimizeToggles.apply(
                false, 0,
                false, false, false,
                false, 0,
                false, false, false, false);
        Recorder plain = new Recorder();
        assertFalse(StableText.isStable(plain));
    }

    @Test
    void isStableFastPathUsedWhenMicroOptsOn() {
        // When microOpts is on, isStable uses instanceof — should return true for StableText
        // wrappers even after clearing the tracked set (the set isn't consulted on the fast path).
        OptimizeToggles.apply(
                false, 0,
                false, false, false,
                false, 0,
                false, false, false, true);
        Recorder inner = new Recorder();
        FormattedCharSequence marked = StableText.mark(inner);
        StableText.clearTracked();
        // Fast path: instanceof check, doesn't consult the set.
        assertTrue(StableText.isStable(marked));
    }

    @Test
    void isStableSlowPathUsedWhenMicroOptsOff() {
        OptimizeToggles.apply(
                false, 0,
                false, false, false,
                false, 0,
                false, false, false, false);
        Recorder inner = new Recorder();
        FormattedCharSequence marked = StableText.mark(inner);
        assertTrue(StableText.isStable(marked));
        StableText.clearTracked();
        // Slow path: after clearing the set, the wrapper is no longer "known stable".
        assertFalse(StableText.isStable(marked));
    }

    @Test
    void acceptDelegatesToInner() {
        OptimizeToggles.apply(
                false, 0,
                false, false, false,
                false, 0,
                false, false, false, false);
        Recorder inner = new Recorder();
        FormattedCharSequence marked = StableText.mark(inner);
        // Pass a sink that just returns true; the marked wrapper should delegate to inner.
        marked.accept((index, style, codePoint) -> true);
        assertTrue(inner.accepted, "StableText.accept should delegate to the wrapped sequence");
    }

    @Test
    void multipleMarksAreTrackedSeparately() {
        OptimizeToggles.apply(
                false, 0,
                false, false, false,
                false, 0,
                false, false, false, false);
        assertEquals(0, StableText.trackedCount());
        StableText.mark(new Recorder());
        StableText.mark(new Recorder());
        StableText.mark(new Recorder());
        assertEquals(3, StableText.trackedCount());
    }
}
