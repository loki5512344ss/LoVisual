package dev.loki.lovisual.features.gui.hud.nondraggable.impl.dynamicisland;

import dev.loki.lovisual.render.engine.animation.AnimationUtility;

/**
 * Анимации Dynamic Island: появление и раскрытие.
 */
final class IslandAnimation {

    float visibilityAnim;
    float expandAnim;
    float widthAnim = -1f;
    float mainWidthAnim = -1f;
    float leftContextWidthAnim;
    float rightContextWidthAnim;
    float heightAnim = -1f;

    void updateVisibility(float dt, boolean shouldShow) {
        float target = shouldShow ? 1f : 0f;
        visibilityAnim = AnimationUtility.approach(visibilityAnim, target, dt, 10f);
        visibilityAnim = AnimationUtility.snap(visibilityAnim, target, 0.002f);
    }

    void updateExpand(float dt, boolean expanded) {
        float target = expanded ? 1f : 0f;
        expandAnim = AnimationUtility.approach(expandAnim, target, dt, 10f);
        expandAnim = AnimationUtility.snap(expandAnim, target, 0.002f);
    }

    void updateLayout(float dt, float targetMainWidth, float targetHeight,
                      float targetLeftContextWidth, float targetRightContextWidth) {
        mainWidthAnim = mainWidthAnim < 0f
                ? targetMainWidth
                : AnimationUtility.approach(mainWidthAnim, targetMainWidth, dt, 12f);
        heightAnim = heightAnim < 0f
                ? targetHeight
                : AnimationUtility.approach(heightAnim, targetHeight, dt, 12f);
        leftContextWidthAnim = AnimationUtility.approach(
                leftContextWidthAnim, targetLeftContextWidth, dt, 13f);
        rightContextWidthAnim = AnimationUtility.approach(
                rightContextWidthAnim, targetRightContextWidth, dt, 13f);
        mainWidthAnim = AnimationUtility.snap(mainWidthAnim, targetMainWidth, 0.05f);
        heightAnim = AnimationUtility.snap(heightAnim, targetHeight, 0.05f);
        leftContextWidthAnim = AnimationUtility.snap(leftContextWidthAnim, targetLeftContextWidth, 0.05f);
        rightContextWidthAnim = AnimationUtility.snap(rightContextWidthAnim, targetRightContextWidth, 0.05f);
    }

    float compactAlpha(float alpha) {
        return (1f - AnimationUtility.easeOutCubic(expandAnim)) * alpha;
    }

    float expandedAlpha(float alpha) {
        return AnimationUtility.easeOutCubic(expandAnim) * alpha;
    }

    float visibilityAlpha() {
        return AnimationUtility.easeOutCubic(visibilityAnim);
    }

    boolean isVisible(float threshold) {
        return visibilityAnim > threshold;
    }

    boolean isExpanded(float threshold) {
        return expandAnim > threshold;
    }

    void resetWidth() {
        widthAnim = -1f;
    }
}
