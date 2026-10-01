package dev.loki.lovisual.features.module.modules.misc.voice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VoiceIndicatorPulseTest {
    @Test
    void ringsStayInRangeAndExpandWhileFading() {
        for (int i = 0; i < VoiceIndicatorPulse.RINGS; i++) {
            for (double t = 0; t < 5; t += 0.07) {
                VoiceIndicatorPulse.Ring r = VoiceIndicatorPulse.ring(t, i, true);
                assertTrue(r.alpha() >= 0f && r.alpha() <= 1f);
                assertTrue(r.radius() >= 0.45f && r.radius() <= 1.2f);
            }
        }
        VoiceIndicatorPulse.Ring early = VoiceIndicatorPulse.ring(0.1, 0, true);
        VoiceIndicatorPulse.Ring late = VoiceIndicatorPulse.ring(0.9, 0, true);
        assertTrue(late.radius() > early.radius());
        assertTrue(late.alpha() < early.alpha());
    }

    @Test
    void staticRingsDoNotAnimate() {
        assertEquals(VoiceIndicatorPulse.ring(0.0, 1, false), VoiceIndicatorPulse.ring(7.3, 1, false));
        assertTrue(VoiceIndicatorPulse.ring(0.0, 0, false).alpha() > 0f);
    }
}
