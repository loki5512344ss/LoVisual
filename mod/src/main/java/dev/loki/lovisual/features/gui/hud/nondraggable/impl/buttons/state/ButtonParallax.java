package dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons.state;

import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.warp.RenderWarpStack;
import net.minecraft.client.gui.components.AbstractWidget;

/**
 * Параллакс кнопок по курсору: поворот и сдвиг контента при наведении.
 */
public final class ButtonParallax {
    private static final float PARALLAX_MAX_ANGLE = 7.0f;
    private static final float PARALLAX_DEPTH = 3.5f;
    private static final float PARALLAX_PERSPECTIVE = 1.0f;
    private static final float PARALLAX_SCALE_BOOST = 0.018f;
    private static final float PARALLAX_CONTENT_SHIFT = 1.35f;

    private ButtonParallax() {
    }

    public static Data compute(AbstractWidget widget, float hover) {
        if (widget == null || hover <= 0.001f || !widget.isActive()) {
            return Data.NONE;
        }

        float w = Math.max(1.0f, widget.getWidth());
        float h = Math.max(1.0f, widget.getHeight());
        float mx = ButtonHoverState.mouseX(widget, widget.getX() + w * 0.5f);
        float my = ButtonHoverState.mouseY(widget, widget.getY() + h * 0.5f);
        float nx = clamp((mx - widget.getX()) / w * 2.0f - 1.0f, -1.0f, 1.0f);
        float ny = clamp((my - widget.getY()) / h * 2.0f - 1.0f, -1.0f, 1.0f);
        float ease = ButtonHoverState.smoothHover(hover);
        if (ease <= 0.001f) {
            return Data.NONE;
        }

        return new Data(
                -nx * PARALLAX_MAX_ANGLE * ease,
                ny * PARALLAX_MAX_ANGLE * ease,
                0.0f,
                1.0f + PARALLAX_SCALE_BOOST * ease,
                nx * PARALLAX_CONTENT_SHIFT * ease,
                ny * PARALLAX_CONTENT_SHIFT * ease,
                true
        );
    }

    public static float shiftX(AbstractWidget widget, float hover) {
        return compute(widget, hover).shiftX;
    }

    public static float shiftY(AbstractWidget widget, float hover) {
        return compute(widget, hover).shiftY;
    }

    public static RenderWarpStack.Scope pushWarp(Data parallax, float x, float y, float w, float h) {
        if (parallax == null || !parallax.active || Math.abs(w) <= 0.0001f || Math.abs(h) <= 0.0001f) {
            return Renderer2D.pushWarp(null);
        }
        return Renderer2D.pushPerspectiveWarp(
                x, y, w, h,
                parallax.yawDeg, parallax.pitchDeg, parallax.rollDeg,
                PARALLAX_DEPTH, PARALLAX_PERSPECTIVE, parallax.scale
        );
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public record Data(float yawDeg,
                       float pitchDeg,
                       float rollDeg,
                       float scale,
                       float shiftX,
                       float shiftY,
                       boolean active) {
        public static final Data NONE = new Data(0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, false);
    }
}
