package dev.loki.lovisual.features.module.modules.visuals.effects.hits.death;

import dev.loki.lovisual.features.module.modules.visuals.effects.hits.death.DeathPhysics.Part;
import dev.loki.lovisual.features.module.modules.visuals.effects.hits.death.DeathPhysics.Style;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.texture.TextureStorage;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.util.player.health.PlayerSkinResolver;
import dev.loki.lovisual.util.time.GameClock;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static dev.loki.lovisual.features.module.modules.combat.targetesp.TargetEspRender.addBillboardVertices;
import static dev.loki.lovisual.features.module.modules.combat.targetesp.TargetEspRender.applyOpacity;

/** Режимы смерти из evo: тело рассыпается на части (Shatter/Explosion), взлетает (Ascend) или тает (Disintegrate). */
public final class DeathEffectHelper {

    private static final int FALLBACK_TINT = 0xFF9A9A9A;
    private static final float SPARK_LIFE = 0.9f;
    private static final float FLASH_LIFE = 0.45f;

    private static final class Spark {
        float x, y, z, vx, vy, vz;
    }

    private static final class Corpse {
        final List<Part> parts;
        final List<Spark> sparks = new ArrayList<>();
        final Style style;
        final long start;
        final float duration;
        final float groundY;
        final boolean bounce;
        final Identifier skin;
        final int sparkColor;
        final float[] center;
        long last;

        Corpse(List<Part> parts, Style style, long start, float duration, float groundY, boolean bounce,
               Identifier skin, int sparkColor, float[] center) {
            this.parts = parts;
            this.style = style;
            this.start = start;
            this.last = start;
            this.duration = duration;
            this.groundY = groundY;
            this.bounce = bounce;
            this.skin = skin;
            this.sparkColor = sparkColor;
            this.center = center;
        }
    }

    private final List<Corpse> corpses = new ArrayList<>();
    private final Random random = new Random();

    public static Style styleOf(String mode) {
        return switch (mode) {
            case "Ascend" -> Style.ASCEND;
            case "Explosion" -> Style.EXPLOSION;
            case "Disintegrate" -> Style.DISINTEGRATE;
            default -> Style.SHATTER;
        };
    }

    public static boolean handles(String mode) {
        return "Shatter".equals(mode) || "Ascend".equals(mode) || "Explosion".equals(mode) || "Disintegrate".equals(mode);
    }

    public void clear() {
        corpses.clear();
    }

    public void spawn(LivingEntity e, Style style, float power, float duration, float spin, boolean bounce,
                      int sparks, int sparkColor) {
        float height = DeathBody.height(e);
        List<Part> parts = DeathBody.build(e.getX(), e.getY(), e.getZ(), e.yBodyRot, height);
        float cx = (float) e.getX(), cy = (float) e.getY() + height * 0.6f, cz = (float) e.getZ();
        for (Part p : parts) DeathPhysics.launch(p, style, power, spin, duration, cx, cy, cz, random);
        Identifier skin = e instanceof AbstractClientPlayer player ? PlayerSkinResolver.resolvePlayerSkin(player) : null;
        Corpse c = new Corpse(parts, style, GameClock.millis(), duration, (float) e.getY(), bounce, skin, sparkColor,
                new float[]{cx, cy, cz});
        if (style == Style.EXPLOSION) {
            for (int i = 0; i < sparks; i++) c.sparks.add(spark(cx, cy, cz, power));
        }
        corpses.add(c);
    }

    private Spark spark(float cx, float cy, float cz, float power) {
        Spark s = new Spark();
        s.x = cx;
        s.y = cy;
        s.z = cz;
        float theta = random.nextFloat() * 6.2831855f;
        float up = random.nextFloat() * 2.0f - 0.6f;
        float speed = (2.0f + random.nextFloat() * 5.0f) * power;
        float horiz = (float) Math.sqrt(Math.max(0.0, 1.0 - up * up * 0.25));
        s.vx = (float) Math.cos(theta) * horiz * speed;
        s.vz = (float) Math.sin(theta) * horiz * speed;
        s.vy = up * speed;
        return s;
    }

    public void render(Renderer3D renderer, float tickDelta) {
        if (corpses.isEmpty() || renderer == null) return;
        long now = GameClock.millis();
        for (Iterator<Corpse> it = corpses.iterator(); it.hasNext(); ) {
            Corpse c = it.next();
            float t = (now - c.start) / 1000.0f;
            if (t > c.duration + 0.2f) {
                it.remove();
                continue;
            }
            advance(c, now, t);
            drawParts(renderer, c, t);
            if (c.style == Style.EXPLOSION) drawSparks(renderer, c, t);
        }
    }

    private void advance(Corpse c, long now, float t) {
        float dt = Math.min(0.1f, (now - c.last) / 1000.0f);
        c.last = now;
        int steps = Math.max(1, (int) Math.ceil(dt / 0.02f));
        float h = dt / steps;
        for (int i = 0; i < steps; i++) {
            for (Part p : c.parts) DeathPhysics.step(p, c.style, h, t, c.groundY, c.bounce);
        }
        for (Spark s : c.sparks) {
            s.vy -= 9.0f * dt;
            s.x += s.vx * dt;
            s.y += s.vy * dt;
            s.z += s.vz * dt;
        }
    }

    private void drawParts(Renderer3D renderer, Corpse c, float t) {
        boolean textured = c.skin != null;
        MeshBuilder mesh = textured
                ? renderer.batchTextured(LoVisualRenderPipelines.WORLD_TEXTURED_DEPTH, c.skin, Renderer3D.DepthMode.MAIN)
                : renderer.batch(LoVisualRenderPipelines.WORLD_COLORED_DEPTH, Renderer3D.DepthMode.MAIN);
        if (mesh == null) return;
        for (Part p : c.parts) {
            float alpha = DeathPhysics.alpha(p, c.style, t, c.duration);
            if (alpha > 0.01f) DeathMesh.draw(mesh, p, DeathBody.layout(p.kind), textured ? 0xFFFFFF : FALLBACK_TINT & 0xFFFFFF, alpha, textured);
        }
    }

    private void drawSparks(Renderer3D renderer, Corpse c, float t) {
        MeshBuilder glow = renderer.batchTextured(LoVisualRenderPipelines.WORLD_TEXTURED_ADDITIVE, TextureStorage.BLOOM,
                Renderer3D.DepthMode.NONE);
        if (glow == null) return;
        Vector3f right = new Vector3f(1, 0, 0).rotate(RenderState.cameraRotation);
        Vector3f up = new Vector3f(0, 1, 0).rotate(RenderState.cameraRotation);
        if (t < FLASH_LIFE) {
            float k = t / FLASH_LIFE;
            float size = 0.6f + k * 3.2f;
            int col = applyOpacity(c.sparkColor, (1.0f - k) * (1.0f - k));
            addBillboardVertices(glow, c.center[0], c.center[1], c.center[2], new Vector3f(right).mul(size),
                    new Vector3f(up).mul(size), col, col, col, col);
        }
        float fade = Math.max(0.0f, 1.0f - t / SPARK_LIFE);
        if (fade <= 0.0f) return;
        int col = applyOpacity(c.sparkColor, fade);
        Vector3f r = new Vector3f(right).mul(0.07f);
        Vector3f u = new Vector3f(up).mul(0.07f);
        for (Spark s : c.sparks) addBillboardVertices(glow, s.x, s.y, s.z, r, u, col, col, col, col);
    }
}
