package dev.loki.lovisual.render.engine.renderer.ui.draw.edge;
import dev.loki.lovisual.render.engine.renderer.ui.draw.effect.UiPathCap;
import dev.loki.lovisual.render.engine.renderer.ui.draw.effect.UiPathJoin;

public record UiStroke(float thickness, UiPathCap cap, UiPathJoin join) {
    public static final UiStroke NONE = new UiStroke(0f, UiPathCap.BUTT, UiPathJoin.MITER);

    public static UiStroke of(double thickness) {
        return new UiStroke((float) Math.max(0.0, thickness), UiPathCap.BUTT, UiPathJoin.MITER);
    }

    public UiStroke withRoundCapsAndJoins() {
        return new UiStroke(thickness, UiPathCap.ROUND, UiPathJoin.ROUND);
    }

    public boolean enabled() {
        return thickness > 0f;
    }
}
