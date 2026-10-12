package dev.loki.lovisual.render.engine.optimize.profile;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link DrawStats} — the F3 draw/pipeline/framebuffer counters.
 *
 * <p>Stage 3 step 1 of {@code mod/docs/optimize-layer-design.md}. Everything
 * runs on the JUnit thread, which stands in for the render thread; time is
 * passed into {@link DrawStats#endFrame(boolean, long)} so the one-second
 * window can be driven without sleeping.
 */
public final class DrawStatsTest {

    @AfterEach
    void reset() {
        DrawStats.reset();
    }

    @Test
    void nothingCountsBeforeTheFirstShownFrame() {
        DrawStats.draw();
        DrawStats.pipeline("pipeline/a");
        DrawStats.bind();

        DrawStats.Snapshot s = DrawStats.snapshot();
        assertEquals(0, s.frames());
        assertEquals(0, s.draws());
        assertTrue(s.top().isEmpty());
    }

    @Test
    void nothingIsPublishedInsideTheWindow() {
        DrawStats.endFrame(true, 0L);
        DrawStats.draw();

        DrawStats.endFrame(true, DrawStats.WINDOW_NS - 1);

        assertEquals(0, DrawStats.snapshot().frames());
        assertEquals(0, DrawStats.snapshot().draws());
    }

    @Test
    void publishesOnceTheWindowElapses() {
        DrawStats.endFrame(true, 0L);
        DrawStats.draw();
        DrawStats.draw();
        DrawStats.pipeline("pipeline/a");
        DrawStats.bind();

        DrawStats.endFrame(true, DrawStats.WINDOW_NS);

        DrawStats.Snapshot s = DrawStats.snapshot();
        assertEquals(1, s.frames());
        assertEquals(2, s.draws());
        assertEquals(1, s.pipelines());
        assertEquals(1, s.binds());
        assertEquals(List.of("pipeline/a 1"), s.top());
    }

    @Test
    void closingF3StopsCountingAndClearsTheSnapshot() {
        DrawStats.endFrame(true, 0L);
        DrawStats.draw();
        DrawStats.endFrame(true, DrawStats.WINDOW_NS);
        assertEquals(1, DrawStats.snapshot().draws());

        DrawStats.endFrame(false, DrawStats.WINDOW_NS + 1);
        assertEquals(0, DrawStats.snapshot().frames());
        assertTrue(DrawStats.snapshot().top().isEmpty());

        // a frame drawn while F3 is closed must not land anywhere
        DrawStats.draw();
        DrawStats.bind();
        assertEquals(0, DrawStats.snapshot().draws());
        assertEquals(0, DrawStats.snapshot().binds());
    }

    @Test
    void publishAveragesOverFramesAndSortsTopBusiestFirst() {
        Map<String, Integer> byPipeline = new LinkedHashMap<>();
        byPipeline.put("pipeline/rare", 2);
        byPipeline.put("pipeline/busy", 40);
        byPipeline.put("pipeline/middle", 10);

        DrawStats.Snapshot s = DrawStats.publish(100, 50, 10, 4, byPipeline, 2);

        assertEquals(25, s.draws());   // 100 / 4
        assertEquals(13, s.pipelines()); // round(50 / 4)
        assertEquals(3, s.binds());      // round(10 / 4)
        assertEquals(4, s.frames());
        assertEquals(List.of("pipeline/busy 10", "pipeline/middle 3"), s.top());
    }

    @Test
    void publishWithNoFramesDoesNotDivideByZero() {
        DrawStats.Snapshot s = DrawStats.publish(7, 3, 1, 0, Map.of(), DrawStats.TOP);

        assertEquals(0, s.frames());
        assertEquals(7, s.draws());
        assertEquals(3, s.pipelines());
        assertEquals(1, s.binds());
        assertTrue(s.top().isEmpty());
    }

    @Test
    void topIsLimitedToTheConfiguredCount() {
        Map<String, Integer> byPipeline = new LinkedHashMap<>();
        for (int i = 0; i < 20; i++) {
            byPipeline.put("pipeline/" + i, i + 1);
        }

        DrawStats.Snapshot s = DrawStats.publish(210, 210, 0, 1, byPipeline, DrawStats.TOP);

        assertEquals(DrawStats.TOP, s.top().size());
        assertEquals("pipeline/19 20", s.top().get(0));
        assertEquals("pipeline/15 16", s.top().get(DrawStats.TOP - 1));
    }
}
