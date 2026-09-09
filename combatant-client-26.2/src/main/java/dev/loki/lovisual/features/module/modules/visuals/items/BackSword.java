package dev.loki.lovisual.features.module.modules.visuals.items;

import dev.loki.lovisual.config.values.*;
import dev.loki.lovisual.features.module.Module;
import dev.loki.lovisual.features.module.ModuleCategory;
import dev.loki.lovisual.features.module.ModuleInfo;
import dev.loki.lovisual.features.module.WorldPhase;
import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.util.time.GameClock;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

//todo Description
@ModuleInfo(id = "backsword", displayName = "BackSword", category = ModuleCategory.VISUALS)
public class BackSword extends Module {

    // базовый подъём катаны, чтобы лежала на спине, а не у ног
    private static final float BACK_SWORD_LIFT = 0.85f;

    private static final String SETTING_TARGET = "target";
    private static final String SETTING_COLOR_MODE = "color_mode";
    private static final String SETTING_CUSTOM_COLOR = "custom_color";
    private static final String SETTING_GLOW = "glow";
    private static final String SETTING_GLOW_LEVEL = "glow_level";
    private static final String SETTING_FILL_ALPHA = "fill_alpha";
    private static final String SETTING_OUTLINE_ALPHA = "outline_alpha";
    private static final String SETTING_OFFSET_Y = "offset_y";
    private static final String SETTING_OFFSET_Z = "offset_z";

    private final ModeValue target = modeSetting(
            "backSwordTarget", SETTING_TARGET, "Self", "Self", "All Players", "Self and Others");
    private final ModeValue colorMode = modeSetting(
            "backSwordColorMode", SETTING_COLOR_MODE, "Theme", "Theme", "Custom", "Rainbow");
    private final RGBColorValue customColor =
            visibleWhen(colorNoAlpha("backSwordCustomColor", SETTING_CUSTOM_COLOR, "#E91E63"), this::isCustomColor);
    private final BooleanValue glow =
            bool("backSwordGlow", SETTING_GLOW, true);
    private final NumberValue<Float> glowLevel =
            visibleWhen(num("backSwordGlowLevel", SETTING_GLOW_LEVEL, 50.0f, 0.0f, 100.0f), glow::get);
    private final NumberValue<Float> fillAlpha =
            num("backSwordFillAlpha", SETTING_FILL_ALPHA, 20.0f, 0.0f, 100.0f);
    private final NumberValue<Float> outlineAlpha =
            num("backSwordOutlineAlpha", SETTING_OUTLINE_ALPHA, 85.0f, 0.0f, 100.0f);
    private final NumberValue<Float> offsetY =
            num("backSwordOffsetY", SETTING_OFFSET_Y, 0.1f, -0.5f, 0.7f);
    private final NumberValue<Float> offsetZ =
            num("backSwordOffsetZ", SETTING_OFFSET_Z, 0.28f, 0.0f, 0.6f);

    @Override
    public WorldPhase getWorldPhase() {
        return WorldPhase.END_MAIN;
    }

    @Override
    public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.level == null || mc.player == null || renderer == null) return;

        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        boolean targetSelf = "Self".equals(target.get()) || "Self and Others".equals(target.get());
        boolean targetOthers = "All Players".equals(target.get()) || "Self and Others".equals(target.get());

        if (!firstPerson && targetSelf && mc.player.isAlive()) {
            renderSword(renderer, mc.player, tickDelta);
        }
        if (targetOthers) {
            for (Player player : mc.level.players()) {
                if (player == mc.player || !player.isAlive()) continue;
                renderSword(renderer, player, tickDelta);
            }
        }
    }

    private void renderSword(Renderer3D renderer, Player player, float tickDelta) {
        Vec3 pos = player.getPosition(tickDelta);
        float bodyYaw = Mth.rotLerp(tickDelta, player.yBodyRotO, player.yBodyRot);
        float offY = offsetY.get();
        float offZ = offsetZ.get();

        MeshBuilder fill = renderer.batch(LoVisualRenderPipelines.WORLD_COLORED_DEPTH, Renderer3D.DepthMode.MAIN);
        MeshBuilder glowMesh = null;
        if (glow.get()) {
            glowMesh = renderer.batch(LoVisualRenderPipelines.WORLD_COLORED_ADDITIVE_DEPTH, Renderer3D.DepthMode.PRE_DEPTH);
        }
        if (fill == null) return;

        int fillA = Math.round(fillAlpha.get() / 100f * 255f);
        int outA = Math.round(outlineAlpha.get() / 100f * 255f);

        Shade fillShade = shade(fillA);
        Shade edgeShade = edgeShade(outA);

        drawKatana(fill, pos, bodyYaw, offY, offZ, fillShade);
        drawKatanaEdge(fill, pos, bodyYaw, offY, offZ, edgeShade, 0.004f);

        if (glowMesh != null) {
            float glowBase = glowLevel.get() / 100f;
            float pulse = (float) (Math.sin(GameClock.millis() / 700.0) * 0.12 + 0.88);
            glowBase *= pulse;
            int layers = 12;
            float maxEx = 0.045f;
            for (int i = 0; i < layers; i++) {
                float t = (float) i / (layers - 1);
                float falloff = (1f - t) * (1f - t);
                int ga = (int) (glowBase * falloff * 180f);
                if (ga <= 1) continue;
                drawKatanaGlow(glowMesh, pos, bodyYaw, offY, offZ, shade(ga), t * maxEx);
            }
        }
    }

    private Shade shade(int alpha) {
        return switch (colorMode.get()) {
            case "Custom" -> {
                int c = customColor.getArgb();
                yield fixedShade(c, alpha);
            }
            case "Rainbow" -> (lx, ly, lz) -> {
                float hue = ((ly + 0.6f) * 0.8f + (GameClock.millis() / 1000f) * 0.15f) % 1.0f;
                int[] rgb = hsvToRgb(hue, 0.85f, 1.0f);
                return argb(rgb[0], rgb[1], rgb[2], alpha);
            };
            default -> {
                yield fixedShade(Theme.theme().accent(), alpha);
            }
        };
    }

    private Shade edgeShade(int alpha) {
        if ("Rainbow".equals(colorMode.get())) return shade(alpha);
        int c = "Custom".equals(colorMode.get()) ? customColor.getArgb() : Theme.theme().accent();
        int r = Math.min(((c >>> 16) & 0xFF) + 40, 255);
        int g = Math.min(((c >>> 8) & 0xFF) + 40, 255);
        int b = Math.min((c & 0xFF) + 40, 255);
        return fixedShade(r, g, b, alpha);
    }

    private static Shade fixedShade(int argb, int alpha) {
        int r = (argb >>> 16) & 0xFF, g = (argb >>> 8) & 0xFF, b = argb & 0xFF;
        return (lx, ly, lz) -> argb(r, g, b, alpha);
    }

    private static Shade fixedShade(int r, int g, int b, int alpha) {
        return (lx, ly, lz) -> argb(r, g, b, alpha);
    }

    private boolean isCustomColor() {
        return "Custom".equals(colorMode.get());
    }

    // ===================== Геометрия катаны =====================

    private static void drawKatana(MeshBuilder mesh, Vec3 pos, float bodyYaw, float offY, float offZ, Shade shade) {
        drawBox(mesh, pos, bodyYaw, offY, offZ, shade, -0.028f, -0.42f, -0.016f, 0.028f, 0.0f, 0.016f);
        drawBox(mesh, pos, bodyYaw, offY, offZ, shade, -0.032f, -0.46f, -0.018f, 0.032f, -0.42f, 0.018f);
        drawBox(mesh, pos, bodyYaw, offY, offZ, shade, -0.085f, -0.005f, -0.034f, 0.085f, 0.005f, 0.034f);
        drawBlade(mesh, pos, bodyYaw, offY, offZ, shade);
    }

    private static void drawBlade(MeshBuilder mesh, Vec3 pos, float bodyYaw, float offY, float offZ, Shade shade) {
        float bw = 0.022f, bBack = 0.006f, bEdge = 0.004f;
        float bx0 = -bw, bx1 = bw, by = 0.005f;
        float bzBack = bBack, bzEdge = -bEdge;

        float mw = 0.015f, mBack = 0.004f, mEdge = 0.002f;
        float mx0 = -mw, mx1 = mw, my = 0.50f;
        float mzBack = mBack, mzEdge = -mEdge;

        float tx = 0f, ty = 1.02f, tz = 0f;

        quad(mesh, pos, bodyYaw, offY, offZ, shade, bx0, by, bzBack, bx1, by, bzBack, mx1, my, mzBack, mx0, my, mzBack);
        quad(mesh, pos, bodyYaw, offY, offZ, shade, bx1, by, bzBack, bx1, by, bzEdge, mx1, my, mzEdge, mx1, my, mzBack);
        quad(mesh, pos, bodyYaw, offY, offZ, shade, bx0, by, bzBack, bx0, by, bzEdge, mx0, my, mzEdge, mx0, my, mzBack);
        quad(mesh, pos, bodyYaw, offY, offZ, shade, bx0, by, bzEdge, bx1, by, bzEdge, mx1, my, mzEdge, mx0, my, mzEdge);

        quad(mesh, pos, bodyYaw, offY, offZ, shade, mx0, my, mzBack, mx1, my, mzBack, tx, ty, tz, tx, ty, tz);
        quad(mesh, pos, bodyYaw, offY, offZ, shade, mx1, my, mzBack, mx1, my, mzEdge, tx, ty, tz, tx, ty, tz);
        quad(mesh, pos, bodyYaw, offY, offZ, shade, mx0, my, mzBack, mx0, my, mzEdge, tx, ty, tz, tx, ty, tz);
        quad(mesh, pos, bodyYaw, offY, offZ, shade, mx0, my, mzEdge, mx1, my, mzEdge, tx, ty, tz, tx, ty, tz);
    }

    private static void drawKatanaEdge(MeshBuilder mesh, Vec3 pos, float bodyYaw, float offY, float offZ, Shade shade, float e) {
        drawBox(mesh, pos, bodyYaw, offY, offZ, shade, -0.028f - e, -0.42f - e, -0.016f - e, 0.028f + e, 0.0f + e, 0.016f + e);
        drawBox(mesh, pos, bodyYaw, offY, offZ, shade, -0.032f - e, -0.46f - e, -0.018f - e, 0.032f + e, -0.42f + e, 0.018f + e);
        drawBox(mesh, pos, bodyYaw, offY, offZ, shade, -0.085f - e, -0.005f - e, -0.034f - e, 0.085f + e, 0.005f + e, 0.034f + e);
        drawBlade(mesh, pos, bodyYaw, offY, offZ, shade);
    }

    private static void drawKatanaGlow(MeshBuilder mesh, Vec3 pos, float bodyYaw, float offY, float offZ, Shade shade, float ex) {
        drawBox(mesh, pos, bodyYaw, offY, offZ, shade, -0.028f - ex, -0.42f - ex, -0.016f - ex, 0.028f + ex, 0.0f + ex, 0.016f + ex);
        drawBox(mesh, pos, bodyYaw, offY, offZ, shade, -0.032f - ex, -0.46f - ex, -0.018f - ex, 0.032f + ex, -0.42f + ex, 0.018f + ex);
        drawBox(mesh, pos, bodyYaw, offY, offZ, shade, -0.085f - ex, -0.005f - ex, -0.034f - ex, 0.085f + ex, 0.005f + ex, 0.034f + ex);

        float bw = 0.022f + ex, bBack = 0.006f + ex, bEdge = 0.004f + ex;
        float mw = 0.015f + ex, mBack = 0.004f + ex, mEdge = 0.002f + ex;
        float bx0 = -bw, bx1 = bw, by = 0.005f - ex;
        float mx0 = -mw, mx1 = mw, my = 0.50f;
        float mzBack = mBack, mzEdge = -mEdge;
        float bzBack = bBack, bzEdge = -bEdge;
        float tx = 0f, ty = 1.02f + ex, tz = 0f;

        quad(mesh, pos, bodyYaw, offY, offZ, shade, bx0, by, bzBack, bx1, by, bzBack, mx1, my, mzBack, mx0, my, mzBack);
        quad(mesh, pos, bodyYaw, offY, offZ, shade, bx1, by, bzBack, bx1, by, bzEdge, mx1, my, mzEdge, mx1, my, mzBack);
        quad(mesh, pos, bodyYaw, offY, offZ, shade, bx0, by, bzBack, bx0, by, bzEdge, mx0, my, mzEdge, mx0, my, mzBack);
        quad(mesh, pos, bodyYaw, offY, offZ, shade, bx0, by, bzEdge, bx1, by, bzEdge, mx1, my, mzEdge, mx0, my, mzEdge);
        quad(mesh, pos, bodyYaw, offY, offZ, shade, mx0, my, mzBack, mx1, my, mzBack, tx, ty, tz, tx, ty, tz);
        quad(mesh, pos, bodyYaw, offY, offZ, shade, mx1, my, mzBack, mx1, my, mzEdge, tx, ty, tz, tx, ty, tz);
        quad(mesh, pos, bodyYaw, offY, offZ, shade, mx0, my, mzBack, mx0, my, mzEdge, tx, ty, tz, tx, ty, tz);
        quad(mesh, pos, bodyYaw, offY, offZ, shade, mx0, my, mzEdge, mx1, my, mzEdge, tx, ty, tz, tx, ty, tz);
    }

    private static void drawBox(MeshBuilder mesh, Vec3 pos, float bodyYaw, float offY, float offZ, Shade shade,
                                float x0, float y0, float z0, float x1, float y1, float z1) {
        quad(mesh, pos, bodyYaw, offY, offZ, shade, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        quad(mesh, pos, bodyYaw, offY, offZ, shade, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        quad(mesh, pos, bodyYaw, offY, offZ, shade, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
        quad(mesh, pos, bodyYaw, offY, offZ, shade, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        quad(mesh, pos, bodyYaw, offY, offZ, shade, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        quad(mesh, pos, bodyYaw, offY, offZ, shade, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
    }

    private static void quad(MeshBuilder mesh, Vec3 pos, float bodyYaw, float offY, float offZ, Shade shade,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3) {
        mesh.ensureQuadCapacity();
        int i0 = addVertex(mesh, pos, bodyYaw, offY, offZ, x0, y0, z0, shade.at(x0, y0, z0));
        int i1 = addVertex(mesh, pos, bodyYaw, offY, offZ, x1, y1, z1, shade.at(x1, y1, z1));
        int i2 = addVertex(mesh, pos, bodyYaw, offY, offZ, x2, y2, z2, shade.at(x2, y2, z2));
        int i3 = addVertex(mesh, pos, bodyYaw, offY, offZ, x3, y3, z3, shade.at(x3, y3, z3));
        mesh.quad(i0, i1, i2, i3);
    }

    private static int addVertex(MeshBuilder mesh, Vec3 pos, float bodyYaw, float offY, float offZ,
                                 float lx, float ly, float lz, int argb) {
        Vec3 w = katanaVertex(lx, ly, lz, pos, bodyYaw, offY, offZ);
        mesh.vec3(w.x, w.y, w.z)
                .color((argb >>> 16) & 0xFF, (argb >>> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF);
        return mesh.next();
    }

    private static Vec3 katanaVertex(float lx, float ly, float lz, Vec3 pos, float bodyYaw, float offY, float offZ) {
        float x = lx * 1.15f, y = ly * 1.15f, z = lz * 1.15f;
        float c45 = 0.7071067812f, s45 = -0.7071067812f;
        float rzx = x * c45 - y * s45;
        float rzy = x * s45 + y * c45;
        float c3 = 0.9986295347f, s3 = -0.0523359562f;
        float rxy = rzy * c3 - z * s3;
        float rxz = rzy * s3 + z * c3;
        float ox = rzx - 0.15f, oy = rxy + offY + BACK_SWORD_LIFT, oz = rxz + offZ;
        float yaw = (float) Math.toRadians(180f - bodyYaw);
        float cy = (float) Math.cos(yaw), sy = (float) Math.sin(yaw);
        float wx = ox * cy + oz * sy;
        float wz = -ox * sy + oz * cy;
        return new Vec3(pos.x + wx, pos.y + oy, pos.z + wz);
    }

    private static int argb(int r, int g, int b, int a) {
        return (Mth.clamp(a, 0, 255) << 24) | (Mth.clamp(r, 0, 255) << 16) | (Mth.clamp(g, 0, 255) << 8) | Mth.clamp(b, 0, 255);
    }

    private static int[] hsvToRgb(float h, float s, float v) {
        int i = (int) (h * 6f);
        float f = h * 6f - i;
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);
        float r, g, b;
        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return new int[]{(int) (r * 255), (int) (g * 255), (int) (b * 255)};
    }

    private interface Shade {
        int at(float lx, float ly, float lz);
    }
}
