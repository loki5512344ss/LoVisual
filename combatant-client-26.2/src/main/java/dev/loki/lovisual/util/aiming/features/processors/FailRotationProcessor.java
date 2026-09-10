/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.aiming.features.processors;
import dev.loki.lovisual.util.aiming.features.processors.RotationProcessor;


import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.events.meta.EventHandler;
import dev.loki.lovisual.events.impl.render.GameTickEvent;
import dev.loki.lovisual.util.aiming.rotation.RotationManager;
import dev.loki.lovisual.util.aiming.rotation.RotationTarget;
import dev.loki.lovisual.util.aiming.data.Rotation;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;

public final class FailRotationProcessor implements RotationProcessor {

    private static final int MIN_TRANSITION_TICKS = 8;
    private static final int UNSAFE_COOLDOWN_TICKS = 4;
    private static final float MAX_SHIFT_STEP = 1.15f;
    private static final Rotation ZERO_SHIFT = new Rotation(0f, 0f);

    public final BooleanValue enabled;
    private final NumberValue<Integer> failRate;
    private final NumberValue<Float> failFactor;
    private final NumberValue<Float> strengthHorizontalMin;
    private final NumberValue<Float> strengthHorizontalMax;
    private final NumberValue<Float> strengthVerticalMin;
    private final NumberValue<Float> strengthVerticalMax;
    private final NumberValue<Integer> transitionMin;
    private final NumberValue<Integer> transitionMax;
    private final BooleanSupplier unsafeWindow;

    private int ticksElapsed = Integer.MAX_VALUE;
    private int currentTransitionDuration = 1;
    private int cooldownTicks;
    private Rotation shiftRotation = ZERO_SHIFT;
    private Rotation appliedShift = ZERO_SHIFT;

    public FailRotationProcessor(BooleanValue enabled,
                                 NumberValue<Integer> failRate,
                                 NumberValue<Float> failFactor,
                                 NumberValue<Float> strengthHorizontalMin,
                                 NumberValue<Float> strengthHorizontalMax,
                                 NumberValue<Float> strengthVerticalMin,
                                 NumberValue<Float> strengthVerticalMax,
                                 NumberValue<Integer> transitionMin,
                                 NumberValue<Integer> transitionMax) {
        this(enabled, failRate, failFactor, strengthHorizontalMin, strengthHorizontalMax,
                strengthVerticalMin, strengthVerticalMax, transitionMin, transitionMax, () -> false);
    }

    public FailRotationProcessor(BooleanValue enabled,
                                 NumberValue<Integer> failRate,
                                 NumberValue<Float> failFactor,
                                 NumberValue<Float> strengthHorizontalMin,
                                 NumberValue<Float> strengthHorizontalMax,
                                 NumberValue<Float> strengthVerticalMin,
                                 NumberValue<Float> strengthVerticalMax,
                                 NumberValue<Integer> transitionMin,
                                 NumberValue<Integer> transitionMax,
                                 BooleanSupplier unsafeWindow) {
        this.enabled = enabled;
        this.failRate = failRate;
        this.failFactor = failFactor;
        this.strengthHorizontalMin = strengthHorizontalMin;
        this.strengthHorizontalMax = strengthHorizontalMax;
        this.strengthVerticalMin = strengthVerticalMin;
        this.strengthVerticalMax = strengthVerticalMax;
        this.transitionMin = transitionMin;
        this.transitionMax = transitionMax;
        this.unsafeWindow = unsafeWindow != null ? unsafeWindow : () -> false;
    }

    private static Rotation stepTowards(Rotation current, Rotation target, float maxStep) {
        return new Rotation(
                step(current.yaw(), target.yaw(), maxStep),
                step(current.pitch(), target.pitch(), maxStep)
        );
    }

    private static float step(float current, float target, float maxStep) {
        float diff = target - current;
        if (Math.abs(diff) <= maxStep) {
            return target;
        }
        return current + Math.copySign(maxStep, diff);
    }

    private static boolean isZero(Rotation rotation) {
        return Math.abs(rotation.yaw()) < 0.001f && Math.abs(rotation.pitch()) < 0.001f;
    }

    private static int random(int min, int max) {
        if (max <= min) return min;
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private static float randomRange(float a, float b) {
        float min = Math.min(a, b);
        float max = Math.max(a, b);
        if (max <= min) return min;
        return (float) ThreadLocalRandom.current().nextDouble(min, max);
    }

    public void tick() {
        if (!enabled.get()) {
            resetState();
            return;
        }

        if (cooldownTicks > 0) {
            cooldownTicks--;
        }

        boolean unsafe = unsafeWindow.getAsBoolean();
        if (unsafe) {
            cooldownTicks = Math.max(cooldownTicks, UNSAFE_COOLDOWN_TICKS);
            if (isInFailState() && ticksElapsed < currentTransitionDuration / 2) {
                ticksElapsed = currentTransitionDuration / 2;
            }
        }

        if (isInFailState()) {
            ticksElapsed++;
            if (!isInFailState()) {
                cooldownTicks = Math.max(cooldownTicks, UNSAFE_COOLDOWN_TICKS);
            }
            return;
        }

        if (unsafe || cooldownTicks > 0) {
            return;
        }

        int chance = ThreadLocalRandom.current().nextInt(0, 101);
        if (failRate.get() > chance) {
            currentTransitionDuration = Math.max(MIN_TRANSITION_TICKS, random(transitionMin.get(), transitionMax.get()) * 2);

            float yawShift = randomRange(strengthHorizontalMin.get(), strengthHorizontalMax.get());
            if (ThreadLocalRandom.current().nextBoolean()) yawShift = -yawShift;

            float pitchShift = randomRange(strengthVerticalMin.get(), strengthVerticalMax.get());
            if (ThreadLocalRandom.current().nextBoolean()) pitchShift = -pitchShift;

            shiftRotation = new Rotation(yawShift, pitchShift);
            ticksElapsed = 0;
        }
    }

    public boolean isInFailState() {
        return enabled.get() && ticksElapsed < currentTransitionDuration;
    }

    @EventHandler(priority = 1000)
    public void onGameTick(GameTickEvent event) {
        tick();
    }

    @Override
    public Rotation process(RotationTarget rotationTarget, Rotation currentRotation, Rotation targetRotation) {
        if (!enabled.get() && isZero(appliedShift)) {
            return targetRotation;
        }

        Rotation desiredShift = ZERO_SHIFT;

        if (enabled.get()
                && isInFailState()
                && !unsafeWindow.getAsBoolean()
                && currentRotation.angleTo(targetRotation) <= 8.0f) {
            Rotation prevRotation = RotationManager.INSTANCE.getPreviousRotation();
            if (prevRotation != null) {
                Rotation serverRotation = RotationManager.INSTANCE.getServerRotation();

                float deltaYaw = (prevRotation.yaw() - serverRotation.yaw()) * failFactor.get();
                float deltaPitch = (prevRotation.pitch() - serverRotation.pitch()) * failFactor.get();
                float pulse = pulse();

                desiredShift = new Rotation(
                        (deltaYaw + shiftRotation.yaw()) * pulse,
                        (deltaPitch + shiftRotation.pitch()) * pulse
                );
            }
        }

        appliedShift = stepTowards(appliedShift, desiredShift, MAX_SHIFT_STEP);
        if (isZero(appliedShift)) {
            return targetRotation;
        }
        return new Rotation(
                targetRotation.yaw() + appliedShift.yaw(),
                targetRotation.pitch() + appliedShift.pitch()
        );
    }

    private float pulse() {
        if (currentTransitionDuration <= 0) {
            return 0.0f;
        }

        float progress = Math.min(1.0f, Math.max(0.0f, (ticksElapsed + 1.0f) / currentTransitionDuration));
        float sine = (float) Math.sin(progress * Math.PI);
        return sine * sine;
    }

    private void resetState() {
        ticksElapsed = Integer.MAX_VALUE;
        cooldownTicks = 0;
        shiftRotation = ZERO_SHIFT;
        appliedShift = ZERO_SHIFT;
    }
}
