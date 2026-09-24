package dev.loki.lovisual.features.gui.hud.nondraggable.impl.hotbar;

import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import net.minecraft.util.Util;

/**
 * Frame-to-frame animation of the hotbar selection rectangle. Tracks which slot
 * is currently "settled" and interpolates towards the newly-clicked one, so
 * {@link HotbarRenderer} can draw a smoothly sliding highlight and lift the
 * incoming item out of its slot.
 */
final class HotbarSelection {

    /** Vertical offset (in GUI pixels) of a normal hotbar item within its slot. */
    static final int ITEM_Y_OFFSET = 2;
    /** Vertical offset of the currently-selected item — a couple pixels higher. */
    static final int ITEM_Y_OFFSET_SELECTED = 0;

    private static final float SELECT_ANIMATION_MS = 145.0f;
    private static final float SELECT_ANIMATION_MIN_PROGRESS = 0.015f;

    private static int animatedSelectedSlot = -1;
    private static float selectionStartSlot = 0.0f;
    private static float selectionTargetSlot = 0.0f;
    private static long selectionAnimStartMs = 0L;

    private HotbarSelection() {
    }

    /** Snapshot of the sliding-selection animation for the current frame. */
    record Frame(float visualSlot, int startSlot, int targetSlot, float progress) {

        Frame(float visualSlot, float startSlot, float targetSlot, float progress) {
            this(visualSlot, clampSlot(Math.round(startSlot)), clampSlot(Math.round(targetSlot)), AnimationUtility.clamp01(progress));
        }

        /** Weight in [0..1] describing how "raised" the given slot should look this frame. */
        float liftFor(int slot) {
            if (startSlot == targetSlot) return slot == targetSlot ? 1.0f : 0.0f;
            if (slot == startSlot) return 1.0f - progress;
            if (slot == targetSlot) return Math.max(progress, SELECT_ANIMATION_MIN_PROGRESS);
            return 0.0f;
        }
    }

    /** Advance the sliding-selection animation towards {@code selectedSlot} and return the current frame. */
    static Frame update(int selectedSlot) {
        long now = Util.getMillis();
        selectedSlot = clampSlot(selectedSlot);
        if (animatedSelectedSlot < 0) {
            animatedSelectedSlot = selectedSlot;
            selectionStartSlot = selectedSlot;
            selectionTargetSlot = selectedSlot;
            selectionAnimStartMs = now;
            return new Frame(selectedSlot, selectedSlot, selectedSlot, 1.0f);
        }
        float visualSlot = currentVisualSlot(now);
        if (selectedSlot != animatedSelectedSlot) {
            selectionStartSlot = visualSlot;
            selectionTargetSlot = selectedSlot;
            selectionAnimStartMs = now;
            animatedSelectedSlot = selectedSlot;
        }
        float progress = progress(now);
        float eased = AnimationUtility.easeOutCubic(progress);
        visualSlot = AnimationUtility.lerp(selectionStartSlot, selectionTargetSlot, eased);
        return new Frame(visualSlot, selectionStartSlot, selectionTargetSlot, eased);
    }

    /** Interpolated Y offset applied to the item in {@code slot} to give the "hop" feel. */
    static float animatedItemYOffset(int slot, Frame frame) {
        return AnimationUtility.lerp(ITEM_Y_OFFSET, ITEM_Y_OFFSET_SELECTED, frame.liftFor(slot));
    }

    private static float currentVisualSlot(long now) {
        float eased = AnimationUtility.easeOutCubic(progress(now));
        return AnimationUtility.lerp(selectionStartSlot, selectionTargetSlot, eased);
    }

    private static float progress(long now) {
        float elapsed = Math.max(0.0f, (float) (now - selectionAnimStartMs));
        if (SELECT_ANIMATION_MS <= 0.0f) return 1.0f;
        return AnimationUtility.clamp01(elapsed / SELECT_ANIMATION_MS);
    }

    private static int clampSlot(int slot) {
        if (slot < 0) return 0;
        if (slot > 8) return 8;
        return slot;
    }
}
