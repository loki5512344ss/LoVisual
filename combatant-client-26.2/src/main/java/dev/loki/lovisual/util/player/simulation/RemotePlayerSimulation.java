/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.player.simulation;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class RemotePlayerSimulation extends PlayerMovementSimulation {

    private final RemotePlayerObservation observation;

    RemotePlayerSimulation(Player player, SimulatedInput input, RemotePlayerObservation observation) {
        super(player, input);
        this.observation = observation;
        primeFlightLikeState(player);
    }

    @Override
    protected boolean shouldUseRemoteFlightPhysics() {
        return observation.isFlightLike();
    }

    @Override
    protected boolean shouldHoldVerticalMotion() {
        return observation.shouldHoldVerticalMotion();
    }

    @Override
    RemotePlayerObservation.MotionKind motionKind() {
        return observation.motionKind();
    }

    private void primeFlightLikeState(Player player) {
        if (!observation.isFlightLike()) {
            return;
        }

        Vec3 measured = observation.measuredVelocity();
        if (measured == null) {
            return;
        }

        // The vanilla simulator derives initial velocity from lastX/Y/Z. For server-side
        // hover/fly this is often exactly the useful value, but vertical motion needs to be
        // clamped or the next predicted ticks will incorrectly turn a hovering target into a
        // falling target.
        if (observation.shouldHoldVerticalMotion() && Math.abs(measured.y) < 0.08) {
            measured = new Vec3(measured.x, 0.0, measured.z);
        }

        setSimulatedVelocity(measured);
        setSimulatedFallDistance(0.0);
        setSimulatedOnGround(false);
        setSimulatedGliding(false);
    }
}
