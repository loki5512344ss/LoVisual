package dev.loki.lovisual.features.gui.hud.draggable.impl.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link MediaPlayerConstants#scrollOffset}, the pure marquee-scroll offset
 * calculator shared by the title and artist rows of the MediaPlayer HUD widget.
 */
final class MediaPlayerConstantsScrollTest {

    @Test
    void staysAtZeroDuringThePause() {
        assertEquals(0f, MediaPlayerConstants.scrollOffset(0f, 100f, 20f, 0.8f));
        assertEquals(0f, MediaPlayerConstants.scrollOffset(0.5f, 100f, 20f, 0.8f));
        assertEquals(0f, MediaPlayerConstants.scrollOffset(0.8f, 100f, 20f, 0.8f));
    }

    @Test
    void slidesLeftAtTheGivenSpeedAfterThePause() {
        float offset = MediaPlayerConstants.scrollOffset(1.8f, 100f, 20f, 0.8f);
        // 1.8s - 0.8s pause = 1.0s of travel at 20px/s.
        assertEquals(-20f, offset, 1e-4f);
    }

    @Test
    void wrapsBackToZeroAtTheStartOfTheNextCycle() {
        // cycleTime = 0.8 + 100/20 = 5.8s; just past that should read like t≈0 again.
        float wrapped = MediaPlayerConstants.scrollOffset(5.9f, 100f, 20f, 0.8f);
        float freshStart = MediaPlayerConstants.scrollOffset(0.1f, 100f, 20f, 0.8f);
        assertEquals(freshStart, wrapped, 1e-4f);
    }

    @Test
    void returnsZeroForNonPositiveSpeed() {
        assertEquals(0f, MediaPlayerConstants.scrollOffset(5f, 100f, 0f, 0.8f));
        assertEquals(0f, MediaPlayerConstants.scrollOffset(5f, 100f, -1f, 0.8f));
    }
}
