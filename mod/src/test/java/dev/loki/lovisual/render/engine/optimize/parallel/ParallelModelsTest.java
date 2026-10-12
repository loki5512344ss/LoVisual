package dev.loki.lovisual.render.engine.optimize.parallel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link ParallelModels} gating — the toggle snapshot, the cores
 * floor and the AnimFreeze interlock. The build itself needs a live render
 * pipeline (posed models, raw vertex consumers) and is covered by an
 * in-game check.
 */
public final class ParallelModelsTest {

    @AfterEach
    void reset() {
        ParallelModels.reset();
    }

    @Test
    void offUntilConfigured() {
        assertFalse(ParallelModels.enabled());
        ParallelModels.configure(true);
        assertTrue(ParallelModels.enabled(), "4+ cores, no Iris, no anim freeze");
        ParallelModels.reset();
        assertFalse(ParallelModels.enabled());
    }

    @Test
    void animFreezeDisablesParallelBuild() {
        ParallelModels.configure(true);
        dev.loki.lovisual.render.engine.optimize.entity.AnimFreeze.configure(true, 16);
        assertFalse(ParallelModels.enabled(), "freeze replays captures, parallel builds fresh — exclusive");
        dev.loki.lovisual.render.engine.optimize.entity.AnimFreeze.reset();
        assertTrue(ParallelModels.enabled());
    }

    @Test
    void minSubmitsMatchesReferenceThreshold() {
        assertEquals(24, ParallelModels.MIN_SUBMITS);
        assertTrue(ParallelModels.FORMAT.getVertexSize() > 0);
    }

    @Test
    void resetFreesWithoutErrorWhenUnused() {
        ParallelModels.configure(true);
        // begin/add/end need real models; reset alone must stay safe on any machine
        ParallelModels.reset();
        assertFalse(ParallelModels.enabled());
    }
}
