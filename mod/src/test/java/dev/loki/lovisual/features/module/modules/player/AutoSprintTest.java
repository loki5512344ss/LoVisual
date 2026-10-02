package dev.loki.lovisual.features.module.modules.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class AutoSprintTest {

    @Test
    void sprintsOnlyForwardByDefault() {
        assertTrue(AutoSprint.wantsSprint(1f, 0f, false, false, false, false));
        assertTrue(AutoSprint.wantsSprint(1f, 1f, false, false, false, false));
        assertFalse(AutoSprint.wantsSprint(0f, 1f, false, false, false, false));
        assertFalse(AutoSprint.wantsSprint(-1f, 0f, false, false, false, false));
        assertFalse(AutoSprint.wantsSprint(0f, 0f, false, false, false, false));
    }

    @Test
    void allDirectionsAcceptsAnyMovement() {
        assertTrue(AutoSprint.wantsSprint(0f, -1f, true, false, false, false));
        assertTrue(AutoSprint.wantsSprint(-1f, 0f, true, false, false, false));
        assertFalse(AutoSprint.wantsSprint(0f, 0f, true, false, false, false));
    }

    @Test
    void sneakingAlwaysCancels() {
        assertFalse(AutoSprint.wantsSprint(1f, 0f, true, true, false, true));
    }

    @Test
    void usingItemCancelsUnlessAllowed() {
        assertFalse(AutoSprint.wantsSprint(1f, 0f, false, false, true, false));
        assertTrue(AutoSprint.wantsSprint(1f, 0f, false, false, true, true));
    }
}
