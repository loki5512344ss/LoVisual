package dev.loki.lovisual.features.platform.presence;

import dev.loki.lovisual.features.platform.presence.PresenceCodec.Gui;
import dev.loki.lovisual.features.platform.presence.PresenceCodec.Sample;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PresenceTest {

    private static Gui gui(int n) {
        List<Sample> samples = new ArrayList<>();
        for (int i = 0; i < n; i++) samples.add(new Sample(i * 10, 255 - i, i % 2 == 0, i));
        return new Gui(2, true, false, false, 7, samples);
    }

    @Test
    void codecRoundTrips() {
        Gui in = gui(5);
        Gui out = PresenceCodec.decode(PresenceCodec.encode(in));
        assertEquals(in, out);
    }

    @Test
    void codecKeepsClosedFlagAndNoCategory() {
        Gui in = new Gui(-1, false, true, true, PresenceCodec.NO_MODULE, List.of());
        assertEquals(in, PresenceCodec.decode(PresenceCodec.encode(in)));
    }

    @Test
    void encodedPayloadFitsServerLimit() {
        assertTrue(PresenceCodec.encode(gui(20)).length() <= 48);
    }

    @Test
    void decodeRejectsGarbage() {
        assertNull(PresenceCodec.decode(null));
        assertNull(PresenceCodec.decode("!!!"));
        assertNull(PresenceCodec.decode(Base64.getEncoder().encodeToString(new byte[]{9, 0, 0, 0})));
        assertNull(PresenceCodec.decode(Base64.getEncoder().encodeToString(new byte[]{1, 0, 0, 5})));
    }

    @Test
    void timelineInterpolatesBetweenSamples() {
        PresenceTimeline tl = new PresenceTimeline();
        Gui g = new Gui(0, false, false, false, 255, List.of(new Sample(0, 0, false, 0), new Sample(100, 200, true, 0)));
        assertTrue(tl.add(1000, g));
        PresenceTimeline.Point p = tl.at(1000 + PresenceTimeline.DELAY_MS - 50);
        assertEquals(50.0f, p.x(), 1e-3f);
        assertEquals(100.0f, p.y(), 1e-3f);
    }

    @Test
    void timelineIgnoresRepeatedPayloadsAndGoesStale() {
        PresenceTimeline tl = new PresenceTimeline();
        Gui g = gui(2);
        assertTrue(tl.add(5000, g));
        assertFalse(tl.add(5000, g));
        assertFalse(tl.add(4000, g));
        assertTrue(tl.live(5000 + 100));
        assertFalse(tl.live(5000 + PresenceTimeline.STALE_MS + 1));
        assertTrue(tl.present(5000 + PresenceTimeline.STALE_MS + 1));
    }

    @Test
    void closedMenuIsNotLiveButStillPresent() {
        PresenceTimeline tl = new PresenceTimeline();
        tl.add(100, new Gui(-1, false, false, true, 255, List.of()));
        assertFalse(tl.live(200));
        assertTrue(tl.present(200));
        assertNull(tl.at(200));
    }
}
