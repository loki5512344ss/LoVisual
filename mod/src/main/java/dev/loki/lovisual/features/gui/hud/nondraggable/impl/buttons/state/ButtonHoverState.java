package dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons.state;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.util.Util;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Hover-анимация кнопок: хранит прогресс наведения и позицию курсора.
 */
public final class ButtonHoverState {
    private static final float HOVER_SPEED_IN = 20.0f;
    private static final float HOVER_SPEED_OUT = 16.0f;

    private static final Map<AbstractWidget, Float> HOVER = new WeakHashMap<>();
    private static final Map<AbstractWidget, Long> LAST_TICK = new WeakHashMap<>();
    private static final Map<AbstractWidget, Float> MOUSE_X = new WeakHashMap<>();
    private static final Map<AbstractWidget, Float> MOUSE_Y = new WeakHashMap<>();

    private ButtonHoverState() {
    }

    public static void clear(AbstractWidget widget) {
        if (widget == null) return;
        HOVER.remove(widget);
        LAST_TICK.remove(widget);
        MOUSE_X.remove(widget);
        MOUSE_Y.remove(widget);
    }

    public static void clearAll() {
        HOVER.clear();
        LAST_TICK.clear();
        MOUSE_X.clear();
        MOUSE_Y.clear();
    }

    public static float updateHover(AbstractWidget widget, boolean hovered) {
        if (widget != null) {
            return updateHover(widget, hovered, widget.getX() + widget.getWidth() * 0.5f, widget.getY() + widget.getHeight() * 0.5f);
        }
        return 0.0f;
    }

    public static float updateHover(AbstractWidget widget, boolean hovered, float mouseX, float mouseY) {
        if (widget == null) return 0.0f;
        if (hovered) {
            MOUSE_X.put(widget, mouseX);
            MOUSE_Y.put(widget, mouseY);
        }

        float target = hovered ? 1.0f : 0.0f;
        float current = HOVER.getOrDefault(widget, 0.0f);

        long now = Util.getMillis();
        long last = LAST_TICK.getOrDefault(widget, now);
        LAST_TICK.put(widget, now);

        float dt = Math.min(0.05f, (now - last) / 1000.0f);
        float speed = hovered ? HOVER_SPEED_IN : HOVER_SPEED_OUT;
        float lerp = 1.0f - (float) Math.exp(-speed * dt);
        current = current + (target - current) * lerp;

        HOVER.put(widget, current);
        return current;
    }

    public static float smoothHover(float hover) {
        float h = Math.max(0.0f, Math.min(1.0f, hover));
        return h * h * (3.0f - 2.0f * h);
    }

    public static float mouseX(AbstractWidget widget, float fallback) {
        return MOUSE_X.getOrDefault(widget, fallback);
    }

    public static float mouseY(AbstractWidget widget, float fallback) {
        return MOUSE_Y.getOrDefault(widget, fallback);
    }

    public static float currentHover(AbstractWidget widget) {
        return HOVER.getOrDefault(widget, 0.0f);
    }
}
