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

    /** Как рисуется лента: ширина и прозрачность по точке, ориентация, мягкие края, ореол и колебания. */
    public interface Style {
        /** true: ширина лежит горизонтально (как лента-флаг); false: лента повёрнута к камере. */
        boolean horizontal();

        /** Края ленты тают в прозрачность вместо плоской полосы. */
        boolean softEdges();

        /** Масштаб ширины отдельного ореола вокруг ленты; 0 без ореола. */
        float glowScale();

        /** Полуширина в точке {@code i} из {@code n}. */
        double halfWidth(float width, int i, int n, RibbonPoint p, float life);

        /** Множитель альфы цвета в точке. */
        float alpha(int i, int n, RibbonPoint p, float life, float cameraDistance);

        /** Смещение точки: {в сторону, по бинормали}; null без колебаний. */
        default float[] motion(RibbonPoint p) {
            return null;
        }
    }

    /** Прежний вид (ProjectileTrails): лента к камере, сужение к хвосту, альфа как life в квадрате. */
    public static final Style LEGACY = new Style() {
        @Override
        public boolean horizontal() {
            return false;
        }

        @Override
        public boolean softEdges() {
            return false;
        }

        @Override
        public float glowScale() {
            return 0.0f;
        }

        @Override
        public double halfWidth(float width, int i, int n, RibbonPoint p, float life) {
            return TrailRibbon.halfWidth(width, life);
        }

        @Override
        public float alpha(int i, int n, RibbonPoint p, float life, float cameraDistance) {
            return life * life;
        }
    };

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

    /** Лента прежнего вида. */
    public static void render(Renderer3D renderer, Collection<TrackBuffer> tracks, long now, long lifeMs, float width,
                              boolean fill, boolean glow, boolean lines, float lineWidth, boolean depth, Vec3 cam,
                              IntBinaryOperator color) {
        render(renderer, tracks, now, lifeMs, width, fill, glow, lines, lineWidth, depth, cam, color, LEGACY);
    }

    /**
     * @param glow  вся заливка складывается аддитивно (прежний «Glow»); ореол стиля рисуется отдельным слоем
     * @param color цвет точки: (индекс от хвоста, число точек) -> ARGB
     */
    public static void render(Renderer3D renderer, Collection<TrackBuffer> tracks, long now, long lifeMs, float width,
                              boolean fill, boolean glow, boolean lines, float lineWidth, boolean depth, Vec3 cam,
                              IntBinaryOperator color, Style style) {
        var depthMode = depth ? Renderer3D.DepthMode.PRE_DEPTH : Renderer3D.DepthMode.MAIN;
        MeshBuilder fillMesh = null;
        if (fill) {
            var pipeline = glow
                    ? (depth ? LoVisualRenderPipelines.WORLD_COLORED_ADDITIVE_DEPTH : LoVisualRenderPipelines.WORLD_COLORED_ADDITIVE)
                    : (depth ? LoVisualRenderPipelines.WORLD_COLORED_LIQUID_IGNORE : LoVisualRenderPipelines.WORLD_COLORED);
            fillMesh = renderer.batch(pipeline, depthMode);
        }
        MeshBuilder glowMesh = null;
        if (fill && style.glowScale() > 0.0f) {
            glowMesh = renderer.batch(depth ? LoVisualRenderPipelines.WORLD_COLORED_ADDITIVE_DEPTH
                    : LoVisualRenderPipelines.WORLD_COLORED_ADDITIVE, depthMode);
        }
        MeshBuilder lineMesh = null;
        if (lines) {
            float prev = RenderState.lineWidth;
            try {
                RenderState.lineWidth = Math.max(0.5f, lineWidth);
                lineMesh = renderer.batch(depth ? LoVisualRenderPipelines.WORLD_COLORED_LINES_LIQUID_IGNORE
                        : LoVisualRenderPipelines.WORLD_COLORED_LINES, depthMode);
            } finally {
                RenderState.lineWidth = prev;
            }
        }
        if (fillMesh == null && lineMesh == null && glowMesh == null) return;
        for (TrackBuffer track : tracks) {
            List<RibbonPoint> pts = track.points();
            if (pts.size() >= 2) draw(pts, now, lifeMs, width, fillMesh, glowMesh, lineMesh, cam, color, style);
        }
    }

    private static void draw(List<RibbonPoint> pts, long now, long lifeMs, float width, MeshBuilder fill,
                             MeshBuilder glowMesh, MeshBuilder line, Vec3 cam, IntBinaryOperator color, Style style) {
        int n = pts.size();
        double[][] center = new double[n][3];
        for (int i = 0; i < n; i++) {
            RibbonPoint p = pts.get(i);
            center[i] = new double[]{p.x(), p.y(), p.z()};
        }
        double[][] side = frame(center, cam, style.horizontal());
        if (style.motion(pts.get(0)) != null) {
            double[][] tangent = tangents(center);
            for (int i = 0; i < n; i++) {
                float[] m = style.motion(pts.get(i));
                if (m == null) continue;
                double bx = side[i][1] * tangent[i][2] - side[i][2] * tangent[i][1];
                double by = side[i][2] * tangent[i][0] - side[i][0] * tangent[i][2];
                double bz = side[i][0] * tangent[i][1] - side[i][1] * tangent[i][0];
                center[i][0] += side[i][0] * m[0] + bx * m[1];
                center[i][1] += side[i][1] * m[0] + by * m[1];
                center[i][2] += side[i][2] * m[0] + bz * m[1];
            }
            side = frame(center, cam, style.horizontal());
        }
        double[][] left = new double[n][3];
        double[][] right = new double[n][3];
        int[] argb = new int[n];
        double[] half = new double[n];
        for (int i = 0; i < n; i++) {
            RibbonPoint p = pts.get(i);
            float life = style == LEGACY ? lifeFraction(now, p.ms(), lifeMs) : RibbonShape.life(now, p.ms(), lifeMs);
            half[i] = style.halfWidth(width, i, n, p, life);
            double dx = center[i][0] - cam.x, dy = center[i][1] - cam.y, dz = center[i][2] - cam.z;
            float mult = style.alpha(i, n, p, life, (float) Math.sqrt(dx * dx + dy * dy + dz * dz));
            argb[i] = scaleAlpha(color.applyAsInt(i, n), mult);
            for (int k = 0; k < 3; k++) {
                left[i][k] = center[i][k] + side[i][k] * half[i];
                right[i][k] = center[i][k] - side[i][k] * half[i];
            }
        }
        boolean soft = style.softEdges();
        for (int i = 1; i < n; i++) {
            if (fill != null) {
                if (soft) softQuad(fill, left[i - 1], center[i - 1], right[i - 1], left[i], center[i], right[i],
                        argb[i - 1], argb[i]);
                else quad(fill, left[i - 1], right[i - 1], right[i], left[i], argb[i - 1], argb[i]);
            }
            if (glowMesh != null) {
                float g = style.glowScale();
                double[] l0 = offset(center[i - 1], side[i - 1], half[i - 1] * g);
                double[] r0 = offset(center[i - 1], side[i - 1], -half[i - 1] * g);
                double[] l1 = offset(center[i], side[i], half[i] * g);
                double[] r1 = offset(center[i], side[i], -half[i] * g);
                softQuad(glowMesh, l0, center[i - 1], r0, l1, center[i], r1,
                        scaleAlpha(argb[i - 1], GLOW_ALPHA), scaleAlpha(argb[i], GLOW_ALPHA));
            }
            if (line != null) {
                segment(line, left[i - 1], left[i], argb[i - 1], argb[i]);
                segment(line, right[i - 1], right[i], argb[i - 1], argb[i]);
            }
        }
    }

    /** Относительная яркость ореола. */
    static final float GLOW_ALPHA = 0.35f;

    private static double[] offset(double[] c, double[] side, double d) {
        return new double[]{c[0] + side[0] * d, c[1] + side[1] * d, c[2] + side[2] * d};
    }

    /** Единичные касательные по центральным разностям соседей. */
    private static double[][] tangents(double[][] c) {
        int n = c.length;
        double[][] out = new double[n][3];
        for (int i = 0; i < n; i++) {
            double[] a = c[Math.max(0, i - 1)], b = c[Math.min(n - 1, i + 1)];
            double dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1e-6) {
                out[i] = new double[]{0.0, 0.0, 1.0};
            } else {
                out[i] = new double[]{dx / len, dy / len, dz / len};
            }
        }
        return out;
    }

    /**
     * Единичный вектор «в сторону» для каждой точки: к камере (касательная x взгляд) или горизонтальный
     * ({@code (-tz, 0, tx)}), с запасным вариантом для вертикального пути.
     */
    static double[][] frame(double[][] c, Vec3 cam, boolean horizontal) {
        int n = c.length;
        double[][] t = tangents(c);
        double[][] out = new double[n][3];
        for (int i = 0; i < n; i++) {
            double sx, sy, sz;
            if (horizontal) {
                sx = -t[i][2];
                sy = 0.0;
                sz = t[i][0];
                if (sx * sx + sz * sz < 1.0E-7) {
                    sx = 1.0;
                    sz = 0.0;
                }
            } else {
                double vx = cam.x - c[i][0], vy = cam.y - c[i][1], vz = cam.z - c[i][2];
                sx = t[i][1] * vz - t[i][2] * vy;
                sy = t[i][2] * vx - t[i][0] * vz;
                sz = t[i][0] * vy - t[i][1] * vx;
            }
            double len = Math.sqrt(sx * sx + sy * sy + sz * sz);
            if (len < 1e-6) len = 1.0;
            out[i] = new double[]{sx / len, sy / len, sz / len};
        }
        return out;
    }

    /** Альфа цвета, умноженная на множитель 0..1; RGB не меняется. */
    static int scaleAlpha(int argb, float mult) {
        float m = mult < 0.0f ? 0.0f : Math.min(mult, 1.0f);
        int alpha = Math.round(((argb >>> 24) & 0xFF) * m);
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

    private static void quad(MeshBuilder m, double[] l0, double[] r0, double[] r1, double[] l1, int c0, int c1) {
        quad4(m, l0, r0, r1, l1, c0, c0, c1, c1);
    }

    private static void quad4(MeshBuilder m, double[] p0, double[] p1, double[] p2, double[] p3,
                              int c0, int c1, int c2, int c3) {
        m.ensureQuadCapacity();
        int a = m.vec3(p0[0], p0[1], p0[2]).color(new RenderColor(c0)).next();
        int b = m.vec3(p1[0], p1[1], p1[2]).color(new RenderColor(c1)).next();
        int c = m.vec3(p2[0], p2[1], p2[2]).color(new RenderColor(c2)).next();
        int d = m.vec3(p3[0], p3[1], p3[2]).color(new RenderColor(c3)).next();
        m.quad(a, b, c, d);
    }

    /** Две полосы: от прозрачного левого края к непрозрачной оси и от оси к прозрачному правому краю. */
    private static void softQuad(MeshBuilder m, double[] l0, double[] k0, double[] r0,
                                 double[] l1, double[] k1, double[] r1, int c0, int c1) {
        int e0 = scaleAlpha(c0, 0.0f), e1 = scaleAlpha(c1, 0.0f);
        quad4(m, l0, k0, k1, l1, e0, c0, c1, e1);
        quad4(m, k0, r0, r1, k1, c0, e0, e1, c1);
    }

    private static void segment(MeshBuilder m, double[] p0, double[] p1, int c0, int c1) {
        m.ensureLineCapacity();
        int a = m.vec3(p0[0], p0[1], p0[2]).color(new RenderColor(c0)).next();
        int b = m.vec3(p1[0], p1[1], p1[2]).color(new RenderColor(c1)).next();
        m.line(a, b);
    }
}
