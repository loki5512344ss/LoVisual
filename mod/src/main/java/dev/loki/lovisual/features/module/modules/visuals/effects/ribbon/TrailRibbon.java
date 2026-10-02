package dev.loki.lovisual.features.module.modules.visuals.effects.ribbon;

import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.List;
import java.util.function.IntBinaryOperator;

/**
 * Лента, повёрнутая к камере: сужается и гаснет к хвосту, с необязательным контуром по краям.
 * Общий рендер для ProjectileTrails и ElytraTrails.
 */
public final class TrailRibbon {

    private TrailRibbon() {
    }

    /** Доля оставшейся жизни точки: 1 свежая, 0 истёкшая. */
    public static float lifeFraction(long now, long ms, long lifeMs) {
        if (lifeMs <= 0) return 0.0f;
        float left = 1.0f - (now - ms) / (float) lifeMs;
        return Math.max(0.0f, Math.min(1.0f, left));
    }

    /** Альфа точки падает как life в квадрате, RGB не меняется. */
    public static int fadeAlpha(int argb, float life) {
        float l = Math.max(0.0f, Math.min(1.0f, life));
        int alpha = Math.round(((argb >>> 24) & 0xFF) * l * l);
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

    /** Полуширина ленты: у свежей точки половина {@code width}, к хвосту сужается до четверти. */
    public static double halfWidth(float width, float life) {
        float l = Math.max(0.0f, Math.min(1.0f, life));
        return width * 0.5 * (0.25 + 0.75 * l);
    }

    /**
     * @param color цвет точки: (индекс от хвоста, число точек) -> ARGB
     */
    public static void render(Renderer3D renderer, Collection<TrackBuffer> tracks, long now, long lifeMs, float width,
                              boolean fill, boolean glow, boolean lines, float lineWidth, boolean depth, Vec3 cam,
                              IntBinaryOperator color) {
        MeshBuilder fillMesh = null;
        if (fill) {
            var pipeline = glow
                    ? (depth ? LoVisualRenderPipelines.WORLD_COLORED_ADDITIVE_DEPTH : LoVisualRenderPipelines.WORLD_COLORED_ADDITIVE)
                    : (depth ? LoVisualRenderPipelines.WORLD_COLORED_LIQUID_IGNORE : LoVisualRenderPipelines.WORLD_COLORED);
            fillMesh = renderer.batch(pipeline, depth ? Renderer3D.DepthMode.PRE_DEPTH : Renderer3D.DepthMode.MAIN);
        }
        MeshBuilder lineMesh = null;
        if (lines) {
            float prev = RenderState.lineWidth;
            try {
                RenderState.lineWidth = Math.max(0.5f, lineWidth);
                lineMesh = renderer.batch(depth ? LoVisualRenderPipelines.WORLD_COLORED_LINES_LIQUID_IGNORE
                        : LoVisualRenderPipelines.WORLD_COLORED_LINES, depth ? Renderer3D.DepthMode.PRE_DEPTH : Renderer3D.DepthMode.MAIN);
            } finally {
                RenderState.lineWidth = prev;
            }
        }
        if (fillMesh == null && lineMesh == null) return;
        for (TrackBuffer track : tracks) {
            List<RibbonPoint> pts = track.points();
            if (pts.size() >= 2) draw(pts, now, lifeMs, width, fillMesh, lineMesh, cam, color);
        }
    }

    private static void draw(List<RibbonPoint> pts, long now, long lifeMs, float width, MeshBuilder fill,
                             MeshBuilder line, Vec3 cam, IntBinaryOperator color) {
        int n = pts.size();
        double[][] left = new double[n][3];
        double[][] right = new double[n][3];
        int[] argb = new int[n];
        for (int i = 0; i < n; i++) {
            RibbonPoint p = pts.get(i);
            RibbonPoint a = pts.get(Math.max(0, i - 1));
            RibbonPoint b = pts.get(Math.min(n - 1, i + 1));
            double dx = b.x() - a.x(), dy = b.y() - a.y(), dz = b.z() - a.z();
            double vx = cam.x - p.x(), vy = cam.y - p.y(), vz = cam.z - p.z();
            double sx = dy * vz - dz * vy, sy = dz * vx - dx * vz, sz = dx * vy - dy * vx;
            double len = Math.sqrt(sx * sx + sy * sy + sz * sz);
            if (len < 1e-6) len = 1.0;
            float life = lifeFraction(now, p.ms(), lifeMs);
            double half = halfWidth(width, life) / len;
            left[i] = new double[]{p.x() + sx * half, p.y() + sy * half, p.z() + sz * half};
            right[i] = new double[]{p.x() - sx * half, p.y() - sy * half, p.z() - sz * half};
            argb[i] = fadeAlpha(color.applyAsInt(i, n), life);
        }
        for (int i = 1; i < n; i++) {
            if (fill != null) quad(fill, left[i - 1], right[i - 1], right[i], left[i], argb[i - 1], argb[i]);
            if (line != null) {
                segment(line, left[i - 1], left[i], argb[i - 1], argb[i]);
                segment(line, right[i - 1], right[i], argb[i - 1], argb[i]);
            }
        }
    }

    private static void quad(MeshBuilder m, double[] l0, double[] r0, double[] r1, double[] l1, int c0, int c1) {
        m.ensureQuadCapacity();
        int a = m.vec3(l0[0], l0[1], l0[2]).color(new RenderColor(c0)).next();
        int b = m.vec3(r0[0], r0[1], r0[2]).color(new RenderColor(c0)).next();
        int c = m.vec3(r1[0], r1[1], r1[2]).color(new RenderColor(c1)).next();
        int d = m.vec3(l1[0], l1[1], l1[2]).color(new RenderColor(c1)).next();
        m.quad(a, b, c, d);
    }

    private static void segment(MeshBuilder m, double[] p0, double[] p1, int c0, int c1) {
        m.ensureLineCapacity();
        int a = m.vec3(p0[0], p0[1], p0[2]).color(new RenderColor(c0)).next();
        int b = m.vec3(p1[0], p1[1], p1[2]).color(new RenderColor(c1)).next();
        m.line(a, b);
    }
}
