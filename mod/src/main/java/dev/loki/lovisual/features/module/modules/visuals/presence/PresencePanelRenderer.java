package dev.loki.lovisual.features.module.modules.visuals.presence;

import dev.loki.lovisual.features.platform.presence.PresenceCodec;
import dev.loki.lovisual.features.platform.presence.PresenceTimeline;
import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a remote player's ClickGUI as a small flat mirror held in front of them: four columns, the one under their
 * cursor lit, the row under the cursor highlighted, the cursor itself and a ring while the button is pressed.
 * Built from coloured quads in a plane spanned by the player's facing, so it needs no text or textures.
 */
final class PresencePanelRenderer {

    private static final float ASPECT = 65.0f / 104.0f;
    private static final float FORWARD = 0.85f;
    private static final float HEIGHT_RATIO = 0.6f;
    private static final int COLUMNS = 4;
    private static final int ROWS = 7;
    private static final float GAP = 0.018f;
    private static final float LAYER = 0.0025f;

    private final float[] a = new float[3];
    private final float[] up = {0.0f, 1.0f, 0.0f};
    private final float[] f = new float[3];
    private final float[] c = new float[3];
    private float width;
    private float height;

    void draw(Renderer3D renderer, Player player, float tickDelta, PresenceTimeline timeline, long now,
              float scale, float alpha) {
        PresenceCodec.Gui gui = timeline.meta();
        PresenceTimeline.Point cursor = timeline.at(now);
        if (gui == null || gui.closed() || cursor == null || alpha <= 0.01f) return;
        MeshBuilder m = renderer.batch(LoVisualRenderPipelines.WORLD_COLORED_DEPTH, Renderer3D.DepthMode.MAIN);
        if (m == null) return;
        place(player, tickDelta, scale);
        int accent = Theme.theme().accent() & 0x00FFFFFF;
        int bg = Theme.theme().windowBg() & 0x00FFFFFF;
        quad(m, 0.0f, 0.0f, 1.0f, 1.0f, 0, rgba(bg, 0.82f * alpha));
        float colW = (1.0f - GAP * (COLUMNS + 1)) / COLUMNS;
        float cx = cursor.x() / 255.0f;
        float cy = cursor.y() / 255.0f;
        for (int i = 0; i < COLUMNS; i++) {
            float x0 = GAP + i * (colW + GAP);
            boolean active = i == gui.category();
            quad(m, x0, 0.04f, x0 + colW, 0.96f, 1, rgba(active ? accent : bg, (active ? 0.28f : 0.5f) * alpha));
            quad(m, x0, 0.04f, x0 + colW, 0.13f, 2, rgba(accent, (active ? 0.7f : 0.3f) * alpha));
            rows(m, i, x0, colW, active, gui, cy, accent, alpha);
        }
        if (gui.searching()) quad(m, 0.3f, -0.1f, 0.7f, -0.03f, 2, rgba(accent, 0.85f * alpha));
        cursor(m, cx, cy, cursor.pressed(), accent, alpha);
    }

    private void rows(MeshBuilder m, int column, float x0, float colW, boolean active, PresenceCodec.Gui gui,
                      float cy, int accent, float alpha) {
        float top = 0.17f;
        float step = (0.95f - top) / ROWS;
        int hot = active && !gui.settings() ? (int) Math.floor((cy - top) / step) : -1;
        for (int r = 0; r < ROWS; r++) {
            float y0 = top + r * step;
            float w = active && gui.settings() ? colW * (0.35f + 0.55f * (((r * 37 + column * 11) % 10) / 10.0f)) : colW * 0.82f;
            boolean lit = r == hot && gui.moduleIndex() != PresenceCodec.NO_MODULE;
            quad(m, x0 + colW * 0.09f, y0, x0 + colW * 0.09f + w, y0 + step * 0.7f, 2,
                    rgba(lit ? accent : 0xFFFFFF, (lit ? 0.75f : 0.1f) * alpha));
        }
    }

    private void cursor(MeshBuilder m, float cx, float cy, boolean pressed, int accent, float alpha) {
        float s = 0.014f;
        quad(m, cx - s, cy - s, cx + s, cy + s, 4, rgba(0xFFFFFF, alpha));
        if (!pressed) return;
        float r = 0.035f;
        float t = 0.006f;
        int col = rgba(accent, 0.9f * alpha);
        quad(m, cx - r, cy - r, cx + r, cy - r + t, 3, col);
        quad(m, cx - r, cy + r - t, cx + r, cy + r, 3, col);
        quad(m, cx - r, cy - r, cx - r + t, cy + r, 3, col);
        quad(m, cx + r - t, cy - r, cx + r, cy + r, 3, col);
    }

    private void place(Player p, float tickDelta, float scale) {
        double yaw = Math.toRadians(p.yBodyRot);
        a[0] = (float) Math.cos(yaw);
        a[1] = 0.0f;
        a[2] = (float) Math.sin(yaw);
        f[0] = (float) -Math.sin(yaw);
        f[1] = 0.0f;
        f[2] = (float) Math.cos(yaw);
        Vec3 pos = p.getPosition(tickDelta);
        float h = p.getBbHeight() * HEIGHT_RATIO;
        c[0] = (float) pos.x + f[0] * FORWARD;
        c[1] = (float) pos.y + h;
        c[2] = (float) pos.z + f[2] * FORWARD;
        width = scale;
        height = scale * ASPECT;
    }

    /** Quad in panel space: u to the right, v downward, both 0..1; {@code layer} lifts it toward the viewer. */
    private void quad(MeshBuilder m, float u0, float v0, float u1, float v1, int layer, int argb) {
        m.ensureQuadCapacity();
        RenderColor col = new RenderColor(argb);
        int i1 = vertex(m, u0, v0, layer, col);
        int i2 = vertex(m, u1, v0, layer, col);
        int i3 = vertex(m, u1, v1, layer, col);
        int i4 = vertex(m, u0, v1, layer, col);
        m.quad(i1, i2, i3, i4);
    }

    private int vertex(MeshBuilder m, float u, float v, int layer, RenderColor col) {
        float du = (u - 0.5f) * width;
        float dv = (0.5f - v) * height;
        float off = layer * LAYER;
        return m.vec3(c[0] + a[0] * du + up[0] * dv + f[0] * off,
                c[1] + a[1] * du + up[1] * dv + f[1] * off,
                c[2] + a[2] * du + up[2] * dv + f[2] * off).color(col).next();
    }

    private static int rgba(int rgb, float alpha) {
        int al = Math.max(0, Math.min(255, Math.round(alpha * 255.0f)));
        return (al << 24) | (rgb & 0x00FFFFFF);
    }
}
