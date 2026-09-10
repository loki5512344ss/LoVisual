/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.model;

import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.HudNotifier;
import dev.loki.lovisual.features.gui.hud.draggable.impl.hudnotifier.ToastGeometry.ToastMetrics;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.features.module.core.Module;

/**
 * Toast state model for HudNotifier: lifecycle timing, preview rotation data, the
 * mutable toast entries rendered by the notifier and the per-frame draw items.
 */
public final class ToastModel {

    public static final int ENTER_MS = 400;
    public static final int DEFAULT_EXIT_MS = 260;
    public static final long PREVIEW_ROTATE_MS = 1000L;
    public static final int TOGGLE_TRANSITION_MS = 420;
    public static final String PREVIEW_KEY = "preview";
    public static final PreviewToast[] PREVIEW_EXAMPLES = new PreviewToast[]{
            new PreviewToast("Module - enabled!", HudNotifier.NotifyType.YES, ToastVisual.MODULE_TOGGLE, true),
            new PreviewToast("Module - disabled!", HudNotifier.NotifyType.NO, ToastVisual.MODULE_TOGGLE, false)
    };

    private ToastModel() {
    }

    public static PreviewToast previewExample(long now) {
        int index = (int) ((Math.max(0L, now) / PREVIEW_ROTATE_MS) % PREVIEW_EXAMPLES.length);
        return PREVIEW_EXAMPLES[index];
    }

    public static long previewStart(long now) {
        long safeNow = Math.max(0L, now);
        return safeNow - safeNow % PREVIEW_ROTATE_MS;
    }

    public static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        float p = t - 1f;
        return 1f + c3 * p * p * p + c1 * p * p;
    }

    public static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    public enum ToastVisual {
        STANDARD,
        MODULE_TOGGLE
    }

    public record PreviewToast(String line, HudNotifier.NotifyType type, ToastVisual visual, Boolean enabled) {
    }

    public static final class Toast {
        public final String key;
        public String line;
        public HudNotifier.NotifyType type;
        public ToastVisual visual;
        public Boolean stateEnabled;
        public long startMs;
        public boolean layoutReady;
        public float layoutX;
        public float layoutY;

        public Toast(String key, String line, HudNotifier.NotifyType type, long startMs) {
            this(key, line, type, startMs, ToastVisual.STANDARD, null);
        }

        public Toast(String key, String line, HudNotifier.NotifyType type, long startMs, ToastVisual visual, Boolean stateEnabled) {
            this.key = key;
            this.line = line;
            this.type = type;
            this.visual = visual == null ? ToastVisual.STANDARD : visual;
            this.stateEnabled = stateEnabled;
            this.startMs = startMs;
        }

        public boolean isExpired(long now, int duration) {
            return now - startMs > duration;
        }

        public float remaining(long now, int duration) {
            if (duration <= 0) return 0f;
            long age = Math.max(0L, now - startMs);
            return 1f - clamp01((float) age / (float) duration);
        }

        public float anim(long now, int duration, int fade) {
            long age = Math.max(0L, now - startMs);
            if (age >= duration) return 0f;

            float in = clamp01((float) age / (float) ENTER_MS);
            float enter = easeOutBack(in);

            int outMs = Math.max(1, fade > 0 ? fade : DEFAULT_EXIT_MS);
            float out = 1f;
            if (age > duration - outMs) {
                out = clamp01((float) (duration - age) / (float) outMs);
            }

            return clamp01(enter * out);
        }

        public float toggleProgress(long now) {
            boolean enabled = stateEnabled != null ? stateEnabled : type == HudNotifier.NotifyType.YES;
            long age = Math.max(0L, now - startMs);
            float t = AnimationUtility.easeInOutCubic((float) age / (float) TOGGLE_TRANSITION_MS);
            return enabled ? t : 1f - t;
        }
    }

    public static final class ToastDraw {
        public final Toast toast;
        public final ToastMetrics metrics;
        public final float remaining;
        public final float anim;
        public final float x;
        public final float y;

        public ToastDraw(Toast toast,
                         ToastMetrics metrics,
                         float remaining,
                         float anim,
                         float x,
                         float y) {
            this.toast = toast;
            this.metrics = metrics;
            this.remaining = remaining;
            this.anim = anim;
            this.x = x;
            this.y = y;
        }
    }
}
