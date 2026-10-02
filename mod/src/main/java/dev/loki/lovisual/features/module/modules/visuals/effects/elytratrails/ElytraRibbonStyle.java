package dev.loki.lovisual.features.module.modules.visuals.effects.elytratrails;

import dev.loki.lovisual.features.module.modules.visuals.effects.ribbon.RibbonPoint;
import dev.loki.lovisual.features.module.modules.visuals.effects.ribbon.RibbonShape;
import dev.loki.lovisual.features.module.modules.visuals.effects.ribbon.TrailRibbon;

/** Вид ленты шлейфа элитры (по evo): горизонтальная лента с сужением, скоростью, плавным стартом и колебаниями. */
final class ElytraRibbonStyle implements TrailRibbon.Style {

    static final float GLOW_WIDTH_SCALE = 2.4f;

    private final RibbonShape.Taper taper;
    private final float taperAmount;
    private final RibbonShape.Motion motion;
    private final float waveSize;
    private final float waveSpeed;
    private final boolean speedReactive;
    private final boolean fadeStart;
    private final boolean softEdges;
    private final boolean glow;

    ElytraRibbonStyle(RibbonShape.Taper taper, float taperAmount, RibbonShape.Motion motion, float waveSize,
                      float waveSpeed, boolean speedReactive, boolean fadeStart, boolean softEdges, boolean glow) {
        this.taper = taper;
        this.taperAmount = taperAmount;
        this.motion = motion;
        this.waveSize = waveSize;
        this.waveSpeed = waveSpeed;
        this.speedReactive = speedReactive;
        this.fadeStart = fadeStart;
        this.softEdges = softEdges;
        this.glow = glow;
    }

    @Override
    public boolean horizontal() {
        return true;
    }

    @Override
    public boolean softEdges() {
        return softEdges;
    }

    @Override
    public float glowScale() {
        return glow ? GLOW_WIDTH_SCALE : 0.0f;
    }

    @Override
    public double halfWidth(float width, int i, int n, RibbonPoint p, float life) {
        return width * 0.5 * RibbonShape.taper(taper, taperAmount, RibbonShape.tailness(i, n))
                * RibbonShape.speedWidth(speedReactive, p.speed());
    }

    @Override
    public float alpha(int i, int n, RibbonPoint p, float life, float cameraDistance) {
        return life * RibbonShape.headRamp(fadeStart, i, n) * RibbonShape.nearFade(cameraDistance)
                * RibbonShape.speedAlpha(speedReactive, p.speed());
    }

    @Override
    public float[] motion(RibbonPoint p) {
        return motion == RibbonShape.Motion.STRAIGHT ? null : RibbonShape.motion(motion, p.ms(), waveSize, waveSpeed);
    }
}
