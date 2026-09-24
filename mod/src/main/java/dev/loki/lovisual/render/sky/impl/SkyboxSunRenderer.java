package dev.loki.lovisual.render.sky;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.scene.visual.ReimaginedVisual;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.MeshRenderer;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.render.engine.uniform.impl.sky.sun.SkySunUniforms;

/**
 * Procedural world-sun pass for {@link CustomSkyboxRenderer}. Computes the sun disc orientation,
 * color and glow from the sun angle / rain / look direction, bakes a billboard quad through
 * {@link MeshBuilder} and draws it with the {@code WORLD_SKY_SUN} pipeline.
 */
final class SkyboxSunRenderer {
    private static final double SKY_SIZE = 160.0;

    private SkyboxSunRenderer() {
    }

    static boolean shouldRender() {
        ReimaginedVisual m = Modules.get(ReimaginedVisual.class);
        return m != null && m.isWorldSunEnabled();
    }

    static void render(Minecraft mc, RenderTarget framebuffer, Camera camera, SkyRenderState sky, float tickDelta) {
        if (mc.level == null) return;

        Vec3 sunDir = new Vec3(-Math.sin(sky.sunAngle), Math.cos(sky.sunAngle), 0.0).normalize();
        float sunVisibility = Mth.clamp(((float) sunDir.y + 0.0625f) / 0.125f, 0.0f, 1.0f);
        if (sunVisibility <= 0.001f) return;

        float dayFactor = Mth.clamp((float) sunDir.y, 0.0f, 1.0f);
        float rainFactor = mc.level.getRainLevel(tickDelta);
        float rainFactor2 = rainFactor * rainFactor;
        float rainFade = 1.0f - rainFactor * 0.7f;
        if (rainFade <= 0.001f) return;

        ReimaginedVisual sunModule = Modules.get(ReimaginedVisual.class);
        float sunSizeSetting = sunModule != null ? sunModule.getWorldSunSize() : 0.06f;
        float sunGlowSetting = sunModule != null ? sunModule.getWorldSunGlow() : 0.18f;
        float sunIntensitySetting = sunModule != null ? sunModule.getWorldSunIntensity() : 1.0f;

        float yawRad = camera.yRot() * ((float) Math.PI / 180.0f);
        float pitchRad = camera.xRot() * ((float) Math.PI / 180.0f);
        Vec3 forward = new Vec3(
                -Math.sin(yawRad) * Math.cos(pitchRad),
                -Math.sin(pitchRad),
                Math.cos(yawRad) * Math.cos(pitchRad)
        ).normalize();
        Vec3 right = new Vec3(0.0, 1.0, 0.0).cross(forward);
        if (right.lengthSqr() < 1.0E-6) {
            right = new Vec3(1.0, 0.0, 0.0);
        } else {
            right = right.normalize();
        }
        Vec3 up = forward.cross(right).normalize();

        double viewDot = sunDir.dot(forward);
        if (viewDot <= -0.2) return;

        double distance = SKY_SIZE * 0.92;
        double coreRadius = SKY_SIZE * (0.045 + sunSizeSetting * 0.95);
        double baseGlowScale = 1.08 + sunGlowSetting * 0.55;
        float lookFactor = Mth.clamp((float) ((viewDot - 0.45) / 0.50), 0.0f, 1.0f);
        lookFactor = lookFactor * lookFactor * (3.0f - 2.0f * lookFactor);
        ReimaginedVisual glareModule = Modules.get(ReimaginedVisual.class);
        float glareFactor = glareModule != null && glareModule.isWorldSunGlareEnabled() ? lookFactor : 0.0f;
        double glowRadius = coreRadius * (baseGlowScale + glareFactor * 1.35);
        double radius = glowRadius;
        Vec3 center = sunDir.scale(distance);

        double angularRadius = Math.max(coreRadius / distance, 0.012);
        float sunrise = 1.0f - Math.abs(dayFactor * 2.0f - 1.0f);
        float noonFactor = 1.0f - sunrise;
        float shadowTimeVar1 = Math.abs(sunVisibility - 0.5f) * 2.0f;
        float shadowTimeVar2 = shadowTimeVar1 * shadowTimeVar1;
        float shadowTime = shadowTimeVar2 * shadowTimeVar2;

        int red = 255;
        int green = Mth.clamp((int) ((0.70f + 0.20f * dayFactor + 0.06f * sunrise) * 255.0f), 0, 255);
        int blue = Mth.clamp((int) ((0.38f + 0.48f * dayFactor - 0.08f * sunrise) * 255.0f), 0, 255);
        float alphaFactor = Mth.clamp(rainFade, 0.0f, 1.0f);
        int alpha = Mth.clamp((int) (alphaFactor * 255.0f), 0, 255);

        float innerRatio = (float) Mth.clamp(coreRadius / glowRadius, 0.08, 0.9);
        float haloStrength = Mth.clamp(
                (0.08f + sunGlowSetting * 0.16f + glareFactor * 0.42f)
                        * sunIntensitySetting * rainFade,
                0.0f,
                1.10f
        );
        float coreStrength = Mth.clamp(1.10f * sunIntensitySetting * rainFade, 0.0f, 2.2f);
        float rayStrength = 0.0f;

        MeshBuilder mesh = new MeshBuilder(LoVisualRenderPipelines.WORLD_SKY_SUN);
        mesh.begin();
        mesh.ensureQuadCapacity();
        int i1 = mesh.vec3(center.x - right.x * radius - up.x * radius, center.y - right.y * radius - up.y * radius, center.z - right.z * radius - up.z * radius)
                .vec2(0.0, 0.0).color(red, green, blue, alpha).next();
        int i2 = mesh.vec3(center.x + right.x * radius - up.x * radius, center.y + right.y * radius - up.y * radius, center.z + right.z * radius - up.z * radius)
                .vec2(1.0, 0.0).color(red, green, blue, alpha).next();
        int i3 = mesh.vec3(center.x + right.x * radius + up.x * radius, center.y + right.y * radius + up.y * radius, center.z + right.z * radius + up.z * radius)
                .vec2(1.0, 1.0).color(red, green, blue, alpha).next();
        int i4 = mesh.vec3(center.x - right.x * radius + up.x * radius, center.y - right.y * radius + up.y * radius, center.z - right.z * radius + up.z * radius)
                .vec2(0.0, 1.0).color(red, green, blue, alpha).next();
        mesh.quad(i1, i2, i3, i4);
        mesh.end();

        SkySunUniforms.update(innerRatio, haloStrength, coreStrength, rayStrength);
        MeshRenderer.begin()
                .attachments(framebuffer)
                .pipeline(LoVisualRenderPipelines.WORLD_SKY_SUN)
                .mesh(mesh)
                .uniform("SkySun", SkySunUniforms.get())
                .end();

    }
}
