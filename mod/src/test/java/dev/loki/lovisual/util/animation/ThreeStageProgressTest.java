package dev.loki.lovisual.util.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ThreeStageProgressTest {

    private static final float EPS = 1.0e-3f;

    @Test
    void stagesFollowElapsedTime() {
        assertEquals(ThreeStageProgress.Stage.APPEAR, ThreeStageProgress.stage(0, 100, 200, 300));
        assertEquals(ThreeStageProgress.Stage.EXIST, ThreeStageProgress.stage(150, 100, 200, 300));
        assertEquals(ThreeStageProgress.Stage.DISAPPEAR, ThreeStageProgress.stage(350, 100, 200, 300));
        assertEquals(ThreeStageProgress.Stage.FINISHED, ThreeStageProgress.stage(700, 100, 200, 300));
    }

    @Test
    void visibilityStartsAtZeroAndEndsAtOneDuringAppear() {
        assertEquals(0.0f, ThreeStageProgress.visibility(0, 100, 200, 300), EPS);
        assertEquals(1.0f, ThreeStageProgress.visibility(100, 100, 200, 300), EPS);
    }

    @Test
    void visibilityStaysFullDuringExist() {
        assertEquals(1.0f, ThreeStageProgress.visibility(150, 100, 200, 300), EPS);
        assertEquals(1.0f, ThreeStageProgress.visibility(299, 100, 200, 300), EPS);
    }

    @Test
    void visibilityFadesToZeroDuringDisappear() {
        assertEquals(1.0f, ThreeStageProgress.visibility(300, 100, 200, 300), EPS);
        assertTrue(ThreeStageProgress.visibility(450, 100, 200, 300) < 1.0f);
        assertEquals(0.0f, ThreeStageProgress.visibility(600, 100, 200, 300), EPS);
    }

    @Test
    void visibilityIsZeroAfterFinished() {
        assertEquals(0.0f, ThreeStageProgress.visibility(1000, 100, 200, 300), EPS);
    }

    @Test
    void isFinishedMatchesStage() {
        assertFalse(ThreeStageProgress.isFinished(0, 100, 200, 300));
        assertFalse(ThreeStageProgress.isFinished(599, 100, 200, 300));
        assertTrue(ThreeStageProgress.isFinished(600, 100, 200, 300));
    }

    @Test
    void zeroDurationStagesDoNotDivideByZero() {
        assertEquals(1.0f, ThreeStageProgress.visibility(0, 0, 200, 300), EPS);
        assertEquals(ThreeStageProgress.Stage.EXIST, ThreeStageProgress.stage(0, 0, 200, 300));
    }
}
