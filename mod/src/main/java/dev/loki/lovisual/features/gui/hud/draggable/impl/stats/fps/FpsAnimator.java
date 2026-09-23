/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl.stats;

import dev.loki.lovisual.render.engine.animation.AnimationUtility;

final class FpsAnimator {

    private FpsAnimator() {
    }

    static String widerText(String a, String b) {
        return a != null && b != null && a.length() < b.length() ? b : a;
    }

    static float smoothWidth(float current, float target) {
        if (current < 0.0f) return target;
        if (target >= current) return target;
        float next = AnimationUtility.approach(current, target, AnimationUtility.deltaTime(), 12.0f);
        return AnimationUtility.snap(next, target, 0.25f);
    }

    static String resolveDisplayedFps(Fps host, String rawValue, long nowMs) {
        if (!host.digitAnimation.get()) {
            host.shownFps = rawValue;
            host.previousShownFps = rawValue;
            host.digitAnimStartMs = 0L;
            host.lastDigitPollMs = nowMs;
            return rawValue;
        }

        if (host.shownFps == null || host.shownFps.isEmpty()) {
            host.shownFps = rawValue;
            host.previousShownFps = rawValue;
            host.digitAnimStartMs = 0L;
            host.lastDigitPollMs = nowMs;
            return host.shownFps;
        }

        if (nowMs - host.lastDigitPollMs < host.digitPollMs.get()) {
            return host.shownFps;
        }

        host.lastDigitPollMs = nowMs;
        if (!rawValue.equals(host.shownFps)) {
            host.previousShownFps = host.shownFps;
            host.shownFps = rawValue;
            host.digitAnimStartMs = nowMs;
        }
        return host.shownFps;
    }

    static float digitProgress(Fps host, long nowMs) {
        if (!host.digitAnimation.get() || host.digitAnimStartMs <= 0L) {
            return 1.0f;
        }
        return AnimationUtility.clamp01(
                (nowMs - host.digitAnimStartMs) / (float) Fps.DIGIT_ANIMATION_DURATION_MS
        );
    }
}
