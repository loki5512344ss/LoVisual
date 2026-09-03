package dev.loki.lovisual.features.module.modules.visuals.lyrics;

import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.text.TextRenderer;
import dev.loki.lovisual.render.engine.text.WorldTextRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class LyricParticle3D {
    private static final double BASE_WORLD_SCALE = 0.0016;

    private final String text;
    private final Vec3 basePosition;
    private final long spawnTimeMs;
    private final long durationMs;
    private boolean forceExpire = false;

    public LyricParticle3D(String text, Vec3 basePosition, long spawnTimeMs, long durationMs) {
        this.text = text;
        this.basePosition = basePosition;
        this.spawnTimeMs = spawnTimeMs;
        this.durationMs = durationMs;
    }

    public Vec3 getBasePosition() {
        return basePosition;
    }

    public void dismiss() {
        this.forceExpire = true;
    }

    public boolean isDead(long nowMs) {
        return forceExpire || (nowMs - spawnTimeMs >= durationMs);
    }

    /**
     * Renders this lyric line as a camera-facing 3D billboard with a selectable entrance/exit animation.
     */
    public void render(Renderer3D renderer, TextRenderer textRenderer, int colorRgb, String animMode, long nowMs) {
        long elapsed = nowMs - spawnTimeMs;
        if (elapsed < 0 || elapsed > durationMs || forceExpire) return;

        float progress = Mth.clamp((float) elapsed / (float) durationMs, 0.0f, 1.0f);

        float alpha = 1.0f;
        double offsetY = 0.0;
        float animScale = 1.0f;
        String displayText = this.text;

        switch (animMode) {
            case "TYPEWRITER" -> {
                if (progress < 0.30f) {
                    float typeProgress = progress / 0.30f;
                    int visibleChars = Math.min(this.text.length(), (int) Math.ceil(this.text.length() * typeProgress));
                    displayText = this.text.substring(0, Math.max(1, visibleChars));
                    alpha = Math.min(1.0f, progress / 0.08f);
                    offsetY = progress * 0.1;
                } else if (progress < 0.75f) {
                    offsetY = progress * 0.1;
                } else {
                    float eraseProgress = (progress - 0.75f) / 0.25f;
                    int remainingChars = Math.max(0, this.text.length() - (int) Math.floor(this.text.length() * eraseProgress));
                    displayText = this.text.substring(0, remainingChars);
                    alpha = 1.0f - (eraseProgress * 0.7f);
                    offsetY = progress * 0.1;
                }
            }
            case "POP_SCALE" -> {
                if (progress < 0.18f) {
                    float t = progress / 0.18f;
                    alpha = easeOutCubic(t);
                    animScale = 0.35f + 0.65f * easeOutBack(t);
                } else if (progress >= 0.80f) {
                    float t = (progress - 0.80f) / 0.20f;
                    alpha = 1.0f - easeInCubic(t);
                    animScale = 1.0f - 0.05f * t;
                }
            }
            case "KINETIC_SLIDE" -> {
                if (progress < 0.20f) {
                    float t = progress / 0.20f;
                    alpha = easeOutCubic(t);
                    offsetY = -0.15 * (1.0f - easeOutCubic(t));
                    animScale = 0.92f + 0.08f * t;
                } else if (progress >= 0.80f) {
                    float t = (progress - 0.80f) / 0.20f;
                    alpha = 1.0f - easeInCubic(t);
                    offsetY = 0.05 * easeInCubic(t);
                    animScale = 1.0f - 0.04f * t;
                }
            }
            case "FADE" -> {
                if (progress < 0.15f) {
                    alpha = progress / 0.15f;
                } else if (progress >= 0.80f) {
                    alpha = 1.0f - ((progress - 0.80f) / 0.20f);
                }
            }
            default -> { // LYRIC_FLOW
                if (progress < 0.15f) {
                    float t = progress / 0.15f;
                    alpha = easeOutCubic(t);
                    offsetY = -0.06 * (1.0f - easeOutCubic(t));
                    animScale = 0.93f + 0.07f * easeOutBack(t);
                } else if (progress >= 0.80f) {
                    float t = (progress - 0.80f) / 0.20f;
                    alpha = 1.0f - easeInCubic(t);
                    offsetY = 0.04 * easeInCubic(t);
                    animScale = 1.0f - 0.03f * t;
                }
            }
        }

        alpha = Mth.clamp(alpha, 0.0f, 1.0f);
        if (alpha <= 0.001f || displayText == null || displayText.isBlank()) return;

        int alphaInt = (int) (alpha * 255.0f);
        RenderColor color = new RenderColor((alphaInt << 24) | (colorRgb & 0x00FFFFFF));

        Vec3 anchor = basePosition.add(0.0, offsetY, 0.0);

        WorldTextRenderer.Options options = WorldTextRenderer.Options.defaults()
                .withColor(color)
                .withCentered(true)
                .withShadow(true)
                .withWorldScale(BASE_WORLD_SCALE * animScale)
                .withDepthMode(Renderer3D.DepthMode.MAIN);

        WorldTextRenderer.drawBillboard(renderer, textRenderer, displayText, anchor, options);
    }

    private static float easeOutCubic(float t) {
        return 1.0f - (float) Math.pow(1.0f - t, 3);
    }

    private static float easeInCubic(float t) {
        return (float) Math.pow(t, 3);
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        return 1.0f + c3 * (float) Math.pow(t - 1.0f, 3) + c1 * (float) Math.pow(t - 1.0f, 2);
    }
}
