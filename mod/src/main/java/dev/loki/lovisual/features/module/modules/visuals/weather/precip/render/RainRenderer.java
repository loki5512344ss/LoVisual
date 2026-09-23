package dev.loki.lovisual.features.module.modules.visuals.weather.precip;

import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Рендер капель, брызг, ряби и тумана.
 */
final class RainRenderer {
    private RainRenderer() {}

    static int packColor(float r, float g, float b, float a) {
        int ai = Mth.clamp(Math.round(a * 255.0f), 0, 255);
        int ri = Mth.clamp(Math.round(r * 255.0f), 0, 255);
        int gi = Mth.clamp(Math.round(g * 255.0f), 0, 255);
        int bi = Mth.clamp(Math.round(b * 255.0f), 0, 255);
        return (ai << 24) | (ri << 16) | (gi << 8) | bi;
    }

    static Vector3f rotated(Quaternionf rot, float x, float y, float z) {
        return new Vector3f(x, y, z).rotate(rot);
    }

    static dev.loki.lovisual.render.engine.color.RenderColor packColorObj(float[] rgb, float a) {
        return new dev.loki.lovisual.render.engine.color.RenderColor(packColor(rgb[0], rgb[1], rgb[2], a));
    }

    static void drawStreaks(Rain rain, MeshBuilder mesh, float camYaw, float[] rgb, float alphaMul, boolean highlight) {
        if (rain.drops.isEmpty()) return;
        float sizeMul = rain.dropSize.get(); float r = rain.radius.get(); float fadeStart = r * 0.62f;
        for (int i = 0, size = rain.drops.size(); i < size; i++) {
            RainParticles.Drop d = rain.drops.get(i);
            double speed = Math.sqrt(d.vx * d.vx + d.vy * d.vy + d.vz * d.vz);
            if (speed < 0.0001) continue;
            float length = (float) Mth.clamp((float) (speed * 0.055 * sizeMul), 0.16f, 2.6f);
            float width = (float) (0.014 + 0.019 * d.mass) * sizeMul * (highlight ? 0.45f : 1.0f);
            float yawRad = (float) Math.toRadians(camYaw);
            double rightX = Math.cos(yawRad); double rightZ = Math.sin(yawRad);
            double horizontal = d.vx * rightX + d.vz * rightZ;
            float tilt = (float) Math.toDegrees(Math.atan2(horizontal, Math.abs(d.vy) + 0.0001));
            tilt = Mth.clamp(tilt, -55.0f, 55.0f);
            Quaternionf orient = new Quaternionf().rotateY((float) Math.toRadians(-camYaw)).rotateZ((float) Math.toRadians(-tilt));
            float half = width * 0.5f; float top = length * 0.5f; float bottom = -length * 0.5f;
            float headAlpha = alphaMul * d.brightness * (highlight ? 0.30f : 0.42f);
            if (headAlpha <= 0.006f) continue;
            headAlpha = Math.min(headAlpha, 0.95f); float tailAlpha = 0.0f;
            float headWidth = half; float tailWidth = half * 0.35f;
            Vector3f p1 = rotated(orient, -tailWidth, top, 0.0f);
            Vector3f p2 = rotated(orient, tailWidth, top, 0.0f);
            Vector3f p3 = rotated(orient, headWidth, bottom, 0.0f);
            Vector3f p4 = rotated(orient, -headWidth, bottom, 0.0f);
            mesh.ensureQuadCapacity();
            int i1 = mesh.vec3(d.x + p1.x, d.y + p1.y, d.z + p1.z).color(packColorObj(rgb, tailAlpha)).next();
            int i2 = mesh.vec3(d.x + p2.x, d.y + p2.y, d.z + p2.z).color(packColorObj(rgb, tailAlpha)).next();
            int i3 = mesh.vec3(d.x + p3.x, d.y + p3.y, d.z + p3.z).color(packColorObj(rgb, headAlpha)).next();
            int i4 = mesh.vec3(d.x + p4.x, d.y + p4.y, d.z + p4.z).color(packColorObj(rgb, headAlpha)).next();
            mesh.quad(i1, i2, i3, i4);
        }
    }

    static void drawRipples(Rain rain, MeshBuilder mesh, float[] rgb, float alphaMul) {
        if (rain.splashList.isEmpty()) return;
        for (int i = 0, size = rain.splashList.size(); i < size; i++) {
            RainParticles.Splash s = rain.splashList.get(i);
            float progress = Mth.clamp(s.life / s.maxLife, 0.0f, 1.0f);
            float eased = 1.0f - (1.0f - progress) * (1.0f - progress);
            float outer = s.scale * (0.18f + eased * (s.water ? 1.35f : 0.85f));
            float inner = outer * (0.55f + 0.35f * eased);
            float alpha = alphaMul * (1.0f - progress) * (s.water ? 0.5f : 0.38f);
            if (alpha <= 0.01f) continue;
            for (int seg = 0; seg < Rain.RIPPLE_SEGMENTS; seg++) {
                double a0 = (Math.PI * 2.0 / Rain.RIPPLE_SEGMENTS) * seg;
                double a1 = (Math.PI * 2.0 / Rain.RIPPLE_SEGMENTS) * (seg + 1);
                float ix0 = (float) (Math.cos(a0) * inner); float iz0 = (float) (Math.sin(a0) * inner);
                float ix1 = (float) (Math.cos(a1) * inner); float iz1 = (float) (Math.sin(a1) * inner);
                float ox0 = (float) (Math.cos(a0) * outer); float oz0 = (float) (Math.sin(a0) * outer);
                float ox1 = (float) (Math.cos(a1) * outer); float oz1 = (float) (Math.sin(a1) * outer);
                mesh.ensureQuadCapacity();
                int v1 = mesh.vec3(s.x + ix0, s.y, s.z + iz0).color(packColorObj(rgb, alpha)).next();
                int v2 = mesh.vec3(s.x + ix1, s.y, s.z + iz1).color(packColorObj(rgb, alpha)).next();
                int v3 = mesh.vec3(s.x + ox1, s.y, s.z + oz1).color(packColorObj(rgb, 0.0f)).next();
                int v4 = mesh.vec3(s.x + ox0, s.y, s.z + oz0).color(packColorObj(rgb, 0.0f)).next();
                mesh.quad(v1, v2, v3, v4);
            }
        }
    }

    static void drawCrowns(Rain rain, MeshBuilder mesh, float camYaw, float[] rgb, float alphaMul) {
        if (rain.splashList.isEmpty()) return;
        Quaternionf orient = new Quaternionf().rotateY((float) Math.toRadians(-camYaw));
        for (int i = 0, size = rain.splashList.size(); i < size; i++) {
            RainParticles.Splash s = rain.splashList.get(i);
            float progress = Mth.clamp(s.life / s.maxLife, 0.0f, 1.0f);
            if (progress > 0.55f) continue;
            float local = progress / 0.55f;
            float height = s.scale * (0.55f * (float) Math.sin(local * Math.PI));
            float alpha = alphaMul * (1.0f - local) * 0.32f;
            if (alpha <= 0.01f || height <= 0.005f) continue;
            float base = s.scale * 0.16f;
            Vector3f p1 = rotated(orient, -base * 0.25f, height, 0.0f);
            Vector3f p2 = rotated(orient, base * 0.25f, height, 0.0f);
            Vector3f p3 = rotated(orient, base, 0.0f, 0.0f);
            Vector3f p4 = rotated(orient, -base, 0.0f, 0.0f);
            mesh.ensureQuadCapacity();
            int i1 = mesh.vec3(s.x + p1.x, s.y + p1.y, s.z + p1.z).color(packColorObj(rgb, 0.0f)).next();
            int i2 = mesh.vec3(s.x + p2.x, s.y + p2.y, s.z + p2.z).color(packColorObj(rgb, 0.0f)).next();
            int i3 = mesh.vec3(s.x + p3.x, s.y + p3.y, s.z + p3.z).color(packColorObj(rgb, alpha)).next();
            int i4 = mesh.vec3(s.x + p4.x, s.y + p4.y, s.z + p4.z).color(packColorObj(rgb, alpha)).next();
            mesh.quad(i1, i2, i3, i4);
        }
    }

    static void drawDroplets(Rain rain, MeshBuilder mesh, float camYaw, float[] rgb, float alphaMul) {
        if (rain.dropletList.isEmpty()) return;
        float sizeMul = rain.dropSize.get(); float yawRad = (float) Math.toRadians(camYaw);
        for (int i = 0, size = rain.dropletList.size(); i < size; i++) {
            RainParticles.Droplet d = rain.dropletList.get(i);
            float progress = Mth.clamp(d.life / d.maxLife, 0.0f, 1.0f);
            float alpha = alphaMul * (1.0f - progress) * 0.5f;
            if (alpha <= 0.01f) continue;
            double speed = Math.sqrt(d.vx * d.vx + d.vy * d.vy + d.vz * d.vz);
            float length = (float) Mth.clamp((float) (speed * 0.03 * sizeMul), 0.04f, 0.4f);
            float width = 0.016f * sizeMul;
            double horizontal = d.vx * Math.cos(yawRad) + d.vz * Math.sin(yawRad);
            float tilt = (float) Math.toDegrees(Math.atan2(horizontal, -d.vy + 0.0001));
            Quaternionf orient = new Quaternionf().rotateY((float) Math.toRadians(-camYaw)).rotateZ((float) Math.toRadians(-tilt));
            Vector3f p1 = rotated(orient, -width * 0.4f, length * 0.5f, 0.0f);
            Vector3f p2 = rotated(orient, width * 0.4f, length * 0.5f, 0.0f);
            Vector3f p3 = rotated(orient, width, -length * 0.5f, 0.0f);
            Vector3f p4 = rotated(orient, -width, -length * 0.5f, 0.0f);
            mesh.ensureQuadCapacity();
            int i1 = mesh.vec3(d.x + p1.x, d.y + p1.y, d.z + p1.z).color(packColorObj(rgb, 0.0f)).next();
            int i2 = mesh.vec3(d.x + p2.x, d.y + p2.y, d.z + p2.z).color(packColorObj(rgb, 0.0f)).next();
            int i3 = mesh.vec3(d.x + p3.x, d.y + p3.y, d.z + p3.z).color(packColorObj(rgb, alpha)).next();
            int i4 = mesh.vec3(d.x + p4.x, d.y + p4.y, d.z + p4.z).color(packColorObj(rgb, alpha)).next();
            mesh.quad(i1, i2, i3, i4);
        }
    }

    static void drawMist(Rain rain, MeshBuilder mesh, Quaternionf camRot, float[] rgb, float alphaMul) {
        if (rain.mistList.isEmpty()) return;
        for (int i = 0, size = rain.mistList.size(); i < size; i++) {
            RainParticles.Mist mist = rain.mistList.get(i);
            float progress = Mth.clamp(mist.life / mist.maxLife, 0.0f, 1.0f);
            float fade = (float) Math.sin(progress * Math.PI);
            float alpha = alphaMul * fade * 0.05f;
            if (alpha <= 0.004f) continue;
            float half = mist.size * 0.5f; float top = mist.size * 0.42f;
            Vector3f right = new Vector3f(1.0f, 0.0f, 0.0f).rotate(camRot);
            Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f).rotate(camRot);
            double bx = mist.x, by = mist.y, bz = mist.z;
            mesh.ensureQuadCapacity();
            int i1 = mesh.vec3(bx - right.x * half, by - right.y * half + up.y * top, bz - right.z * half).color(packColorObj(rgb, 0.0f)).next();
            int i2 = mesh.vec3(bx + right.x * half, by + right.y * half + up.y * top, bz + right.z * half).color(packColorObj(rgb, 0.0f)).next();
            int i3 = mesh.vec3(bx + right.x * half, by + right.y * half, bz + right.z * half).color(packColorObj(rgb, alpha)).next();
            int i4 = mesh.vec3(bx - right.x * half, by - right.y * half, bz - right.z * half).color(packColorObj(rgb, alpha)).next();
            mesh.quad(i1, i2, i3, i4);
        }
    }
}
