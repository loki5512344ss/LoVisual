/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.animation.hand;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Покадровое состояние: одна правка цели за кадр, реакции на смену мира/игрока. */
public final class HandAnimStateTest {

    private static final Object PLAYER_A = new Object();
    private static final Object PLAYER_B = new Object();
    private static final Object LEVEL_1 = new Object();
    private static final Object LEVEL_2 = new Object();

    private static final float DT = 0.5f;

    @Test
    void firstFrameDoesNotProduceSwayImpulse() {
        HandAnimState state = new HandAnimState();
        state.beginFrame(PLAYER_A, LEVEL_1, 30.0f, -15.0f, false, false, DT);
        assertEquals(0.0f, state.sway().yawLag(), 1.0e-6f);
        state.beginFrame(PLAYER_A, LEVEL_1, 30.0f, -15.0f, false, false, DT);
        assertEquals(0.0f, state.sway().yawLag(), 1.0e-6f);
    }

    @Test
    void swayReactsToMouseMotion() {
        HandAnimState state = new HandAnimState();
        state.beginFrame(PLAYER_A, LEVEL_1, 0.0f, 0.0f, false, false, DT);
        for (int i = 0; i < 8; i++) {
            state.beginFrame(PLAYER_A, LEVEL_1, 6.0f * (i + 1), 0.0f, false, false, DT);
        }
        assertTrue(state.sway().yawLag() > 0.0f, "вращение камеры должно давать отставание");
    }

    @Test
    void yawWrapMatchesEquivalentTurn() {
        HandAnimState acrossBoundary = new HandAnimState();
        acrossBoundary.beginFrame(PLAYER_A, LEVEL_1, 175.0f, 0.0f, false, false, DT);
        acrossBoundary.beginFrame(PLAYER_A, LEVEL_1, -175.0f, 0.0f, false, false, DT);

        HandAnimState plain = new HandAnimState();
        plain.beginFrame(PLAYER_A, LEVEL_1, -5.0f, 0.0f, false, false, DT);
        plain.beginFrame(PLAYER_A, LEVEL_1, 5.0f, 0.0f, false, false, DT);

        assertEquals(plain.sway().yawLag(), acrossBoundary.sway().yawLag(), 1.0e-4f,
                "переход через границу мира равен обычному повороту на те же 10 градусов");
    }

    @Test
    void teleportResetsSwayInsteadOfImpulse() {
        HandAnimState state = new HandAnimState();
        state.beginFrame(PLAYER_A, LEVEL_1, 0.0f, 0.0f, false, false, DT);
        for (int i = 0; i < 8; i++) {
            state.beginFrame(PLAYER_A, LEVEL_1, 6.0f * (i + 1), 0.0f, false, false, DT);
        }
        assertTrue(state.sway().yawLag() > 0.5f);
        state.beginFrame(PLAYER_A, LEVEL_1, 180.0f, 0.0f, false, false, DT);
        assertEquals(0.0f, state.sway().yawLag(), 1.0e-6f);
    }

    @Test
    void driftIsPerHand() {
        HandAnimState state = new HandAnimState();
        state.beginFrame(PLAYER_A, LEVEL_1, 0.0f, 0.0f, true, false, DT);
        for (int i = 0; i < 20; i++) {
            state.beginFrame(PLAYER_A, LEVEL_1, 0.0f, 0.0f, true, false, DT);
        }
        assertTrue(state.drift(true) > 0.9f, "дрейф в главной руке должен подняться к единице");
        assertEquals(0.0f, state.drift(false), 1.0e-6f, "вторая рука не должна дёргаться");
    }

    @Test
    void driftSettlesBackWhenUsageStops() {
        HandAnimState state = new HandAnimState();
        state.beginFrame(PLAYER_A, LEVEL_1, 0.0f, 0.0f, true, false, DT);
        for (int i = 0; i < 30; i++) {
            state.beginFrame(PLAYER_A, LEVEL_1, 0.0f, 0.0f, true, false, DT);
        }
        for (int i = 0; i < 120; i++) {
            state.beginFrame(PLAYER_A, LEVEL_1, 0.0f, 0.0f, false, false, DT);
        }
        assertEquals(0.0f, state.drift(true), 1.0e-3f, "после окончания использования дрейф должен уйти в ноль");
    }

    @Test
    void joinProgressRisesThenStays() {
        HandAnimState state = new HandAnimState();
        state.beginFrame(PLAYER_A, LEVEL_1, 0.0f, 0.0f, false, false, DT);
        float previous = 0.0f;
        for (int i = 0; i < 200; i++) {
            state.beginFrame(PLAYER_A, LEVEL_1, 0.0f, 0.0f, false, false, DT);
            float value = state.joinProgress();
            assertTrue(value >= previous - 1.0e-6f, "прогресс появления не должен откатываться");
            previous = value;
        }
        assertEquals(1.0f, state.joinProgress(), 1.0e-3f);
    }

    @Test
    void playerOrLevelChangeRestartsJoin() {
        HandAnimState state = new HandAnimState();
        state.beginFrame(PLAYER_A, LEVEL_1, 0.0f, 0.0f, false, false, DT);
        for (int i = 0; i < 200; i++) {
            state.beginFrame(PLAYER_A, LEVEL_1, 0.0f, 0.0f, false, false, DT);
        }
        assertEquals(1.0f, state.joinProgress(), 1.0e-3f);

        state.beginFrame(PLAYER_B, LEVEL_1, 0.0f, 0.0f, false, false, DT);
        assertTrue(state.joinProgress() < 0.1f, "смена игрока (респавн) перезапускает появление");
        state.beginFrame(PLAYER_B, LEVEL_2, 0.0f, 0.0f, false, false, DT);
        assertTrue(state.joinProgress() < 0.1f, "смена мира тоже перезапускает появление");
    }

    @Test
    void nullPlayerResetsEverything() {
        HandAnimState state = new HandAnimState();
        state.beginFrame(PLAYER_A, LEVEL_1, 0.0f, 0.0f, true, false, DT);
        for (int i = 0; i < 30; i++) {
            state.beginFrame(PLAYER_A, LEVEL_1, 0.0f, 0.0f, true, false, DT);
        }
        state.beginFrame(null, null, 0.0f, 0.0f, false, false, DT);
        assertEquals(0.0f, state.drift(true), 1.0e-6f);
        assertEquals(0.0f, state.joinProgress(), 1.0e-6f);
        assertTrue(state.sway().isSettled());
    }

    @Test
    void resetReturnsToFreshState() {
        HandAnimState state = new HandAnimState();
        state.beginFrame(PLAYER_A, LEVEL_1, 0.0f, 0.0f, true, false, DT);
        for (int i = 0; i < 30; i++) {
            state.beginFrame(PLAYER_A, LEVEL_1, 0.0f, 0.0f, true, false, DT);
        }
        state.reset();
        assertEquals(0.0f, state.drift(true), 1.0e-6f);
        assertEquals(0.0f, state.joinProgress(), 1.0e-6f);
        state.beginFrame(PLAYER_A, LEVEL_1, 0.0f, 0.0f, false, false, DT);
        assertEquals(0.0f, state.sway().yawLag(), 1.0e-6f);
    }
}
