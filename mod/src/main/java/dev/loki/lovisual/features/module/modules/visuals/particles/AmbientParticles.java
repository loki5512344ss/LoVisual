package dev.loki.lovisual.features.module.modules.visuals.particles;

import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.animation.AnimatedColorModeOptions;
import dev.loki.lovisual.render.engine.animation.AnimatedRenderColors;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.texture.TextureStorage;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Атмосферные частицы мира: мягко плывущие вверх светящиеся мошки ("Ambient")
 * или странствующие светлячки со шлейфом ("Firefly", идея — Soup
 * particles/types/FireFlyParticle.java, переписана без trail-объектов на
 * кольцевой буфер позиций). Референс AmbientParticles.java (775 строк, 19
 * визуальных стилей Stars/Hearts/Bloom/.../Pyramid) не портирован целиком —
 * это asset-тяжёлый фича-крип чит-клиента; взята только атмосферная идея
 * (мошки + светлячки), под наши текстуры и правила.
 */
@ModuleInfo(
        id = "ambientparticles",
        displayName = "AmbientParticles",
        category = ModuleCategory.VISUALS,
        description = "Атмосферные частицы мира: плывущие мошки или странствующие светлячки со шлейфом"
)
public final class AmbientParticles extends Module {

    private static final int TRAIL_CAPACITY = 60;
    private static final float MOTE_LIFE_MS = 6000f;

    private final EnumValue<ParticleMode> mode = enumMode("mode", ParticleMode.AMBIENT, ParticleMode.values());
    private final NumberValue<Integer> count = num("count", 60, 10, 200);
    private final NumberValue<Float> scale = num("scale", 1.0f, 0.3f, 3.0f);
    private final NumberValue<Float> spawnRadius = num("spawn_radius", 25.0f, 10.0f, 40.0f);
    private final NumberValue<Float> spawnHeight = num("spawn_height", 6.0f, 2.0f, 20.0f);
    private final NumberValue<Integer> trailLength = visibleWhen(num("trail_length", 20, 5, 60), this::isFirefly);

    private final ModeValue colorMode = modeSetting("colorMode", "color_mode", "Static", AnimatedColorModeOptions.OPTIONS);
    private final NumberValue<Integer> colorSpeed = num("color_speed", 18, 2, 54);
    private final RGBAColorValue colorValue = color("color", "#FFFFE6A0");
    private final RGBAColorValue colorValue2 = visibleWhen(color("color2", "#FF89C4FF"), this::usesSecondaryColor);

    private final List<Mote> motes = new ArrayList<>();
    private final List<Firefly> fireflies = new ArrayList<>();
    private final Random random = new Random();

    private boolean isFirefly() {
        return mode.get() == ParticleMode.FIREFLY;
    }

    private boolean usesSecondaryColor() {
        return AnimatedRenderColors.usesSecondary(AnimatedColorModeOptions.parse(colorMode.get()));
    }

    private int resolveColorArgb(int phase) {
        return AnimatedRenderColors.resolve(
                AnimatedColorModeOptions.parse(colorMode.get()), colorSpeed.get(), phase,
                colorValue.getArgb(), colorValue2.getArgb());
    }

    @Override
    public void onEnable() {
        motes.clear();
        fireflies.clear();
    }

    @Override
    public void onDisable() {
        motes.clear();
        fireflies.clear();
    }

    @Override
    public WorldPhase getWorldPhase() {
        return WorldPhase.END_MAIN;
    }

    @Override
    public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.player == null || mc.level == null) return;

        Vec3 center = mc.player.position();
        if (isFirefly()) {
            fireflies.removeIf(f -> f.age <= 0);
            spawnFireflies(center);
            for (Firefly f : fireflies) f.update(random);
            renderFireflies(renderer);
        } else {
            motes.removeIf(m -> System.currentTimeMillis() - m.spawnMs > MOTE_LIFE_MS);
            spawnMotes(center);
            for (Mote m : motes) m.update();
            renderMotes(renderer);
        }
    }

    private void spawnMotes(Vec3 center) {
        int target = count.get();
        while (motes.size() < target) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = Math.sqrt(random.nextDouble()) * spawnRadius.get();
            double x = center.x + Math.cos(angle) * dist;
            double z = center.z + Math.sin(angle) * dist;
            double y = center.y + random.nextDouble() * spawnHeight.get();
            motes.add(new Mote(x, y, z));
        }
    }

    private void spawnFireflies(Vec3 center) {
        int target = count.get() / 2;
        while (fireflies.size() < target) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = Math.sqrt(random.nextDouble()) * spawnRadius.get();
            double x = center.x + Math.cos(angle) * dist;
            double z = center.z + Math.sin(angle) * dist;
            double y = center.y + random.nextDouble() * spawnHeight.get();
            fireflies.add(new Firefly(x, y, z, trailLength.get(), random));
        }
    }

    private void renderMotes(Renderer3D renderer) {
        if (motes.isEmpty()) return;
        MeshBuilder mesh = renderer.batchTextured(LoVisualRenderPipelines.WORLD_TEXTURED_ADDITIVE, TextureStorage.FIRE_FLY, Renderer3D.DepthMode.MAIN);
        if (mesh == null) return;

        Quaternionf camRot = RenderState.cameraRotation;
        for (Mote m : motes) {
            float progress = (float) ((System.currentTimeMillis() - m.spawnMs) / MOTE_LIFE_MS);
            float fade = (float) Math.sin(Math.min(1.0, progress) * Math.PI);
            if (fade <= 0.01f) continue;
            int argb = applyOpacity(resolveColorArgb((int) (m.x * 10)), fade);
            addBillboard(mesh, m.x, m.y, m.z, scale.get() * 0.35f, camRot, argb);
        }
    }

    private void renderFireflies(Renderer3D renderer) {
        if (fireflies.isEmpty()) return;
        MeshBuilder mesh = renderer.batchTextured(LoVisualRenderPipelines.WORLD_TEXTURED_ADDITIVE, TextureStorage.FIRE_FLY, Renderer3D.DepthMode.MAIN);
        if (mesh == null) return;

        Quaternionf camRot = RenderState.cameraRotation;
        for (Firefly f : fireflies) {
            float bodyFade = Mth.clamp(f.age / (float) f.maxAge, 0f, 1f);
            int size = f.trailSize();
            for (int i = 0; i < size; i++) {
                float trailFade = (i + 1f) / size;
                float alpha = bodyFade * trailFade;
                if (alpha <= 0.02f) continue;
                int argb = applyOpacity(resolveColorArgb(f.hashCode() % 360), alpha);
                double[] p = f.trailPoint(i);
                addBillboard(mesh, p[0], p[1], p[2], scale.get() * (0.12f + 0.08f * trailFade), camRot, argb);
            }
        }
    }

    private static void addBillboard(MeshBuilder mesh, double cx, double cy, double cz,
                                     float size, Quaternionf camRot, int argb) {
        Vector3f right = new Vector3f(1, 0, 0).rotate(camRot).mul(size);
        Vector3f up = new Vector3f(0, 1, 0).rotate(camRot).mul(size);

        mesh.ensureQuadCapacity();
        int i1 = mesh.vec3(cx - right.x() - up.x(), cy - right.y() - up.y(), cz - right.z() - up.z()).vec2(0, 1).color(new RenderColor(argb)).next();
        int i2 = mesh.vec3(cx + right.x() - up.x(), cy + right.y() - up.y(), cz + right.z() - up.z()).vec2(1, 1).color(new RenderColor(argb)).next();
        int i3 = mesh.vec3(cx + right.x() + up.x(), cy + right.y() + up.y(), cz + right.z() + up.z()).vec2(1, 0).color(new RenderColor(argb)).next();
        int i4 = mesh.vec3(cx - right.x() + up.x(), cy - right.y() + up.y(), cz - right.z() + up.z()).vec2(0, 0).color(new RenderColor(argb)).next();
        mesh.quad(i1, i2, i3, i4);
    }

    private static int applyOpacity(int argb, float opacity) {
        int a = (argb >>> 24) & 0xFF;
        int na = Mth.clamp((int) (a * Mth.clamp(opacity, 0f, 1f)), 0, 255);
        return (na << 24) | (argb & 0x00FFFFFF);
    }

    public enum ParticleMode implements EnumValue.IdProvider {
        AMBIENT("ambient"),
        FIREFLY("firefly");

        private final String id;

        ParticleMode(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }
    }

    /** Мошка: плывёт вверх, слегка виляя, живёт {@link #MOTE_LIFE_MS} и гаснет. */
    private static final class Mote {
        double x, y, z;
        final long spawnMs;
        final double phase;

        Mote(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.spawnMs = System.currentTimeMillis();
            this.phase = Math.random() * Math.PI * 2.0;
        }

        void update() {
            double t = (System.currentTimeMillis() - spawnMs) / 1000.0;
            y += 0.004;
            x += Math.sin(t * 0.6 + phase) * 0.003;
            z += Math.cos(t * 0.5 + phase) * 0.003;
        }
    }

    /** Светлячок: случайное блуждание с затуханием скорости и кольцевым буфером шлейфа. */
    private static final class Firefly {
        double x, y, z;
        double vx, vy, vz;
        int age;
        final int maxAge;
        private final double[] trailX;
        private final double[] trailY;
        private final double[] trailZ;
        private int trailCount;
        private int trailHead;

        Firefly(double x, double y, double z, int trailLength, Random random) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.vx = (random.nextDouble() * 2.0 - 1.0) * 0.02;
            this.vy = (random.nextDouble() * 2.0 - 1.0) * 0.01;
            this.vz = (random.nextDouble() * 2.0 - 1.0) * 0.02;
            this.age = 100 + random.nextInt(200);
            this.maxAge = this.age;
            int cap = Mth.clamp(trailLength, 1, TRAIL_CAPACITY);
            this.trailX = new double[cap];
            this.trailY = new double[cap];
            this.trailZ = new double[cap];
        }

        void update(Random random) {
            age--;
            x += vx;
            y += vy;
            z += vz;
            vx += (random.nextDouble() * 2.0 - 1.0) * 0.004;
            vy += (random.nextDouble() * 2.0 - 1.0) * 0.002;
            vz += (random.nextDouble() * 2.0 - 1.0) * 0.004;
            vx *= 0.97;
            vy *= 0.97;
            vz *= 0.97;

            trailX[trailHead] = x;
            trailY[trailHead] = y;
            trailZ[trailHead] = z;
            trailHead = (trailHead + 1) % trailX.length;
            if (trailCount < trailX.length) trailCount++;
        }

        int trailSize() {
            return trailCount;
        }

        double[] trailPoint(int ageIndex) {
            int idx = Math.floorMod(trailHead - 1 - ageIndex, trailX.length);
            return new double[]{trailX[idx], trailY[idx], trailZ[idx]};
        }
    }
}
