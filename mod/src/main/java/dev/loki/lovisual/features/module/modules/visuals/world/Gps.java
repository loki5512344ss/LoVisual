package dev.loki.lovisual.features.module.modules.visuals.world;

import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.HudPhase;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.helpers.clip.ScreenProjection;
import dev.loki.lovisual.util.world.GpsBearing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * Компас-стрелка к заданной точке мира (луч/маркер направления, референс —
 * идея Soup Gps): всегда видимая стрелка на экране, доворачивающаяся к цели
 * по мере поворота камеры, плюс маркер прямо на месте, когда точка попадает
 * в кадр. Цель ставится командой {@code %gps <x> <y> <z>} / {@code %gps clear}
 * (см. {@link dev.loki.lovisual.features.command.impl.world.GpsCommand}).
 */
@ModuleInfo(
        id = "gps",
        displayName = "Gps",
        category = ModuleCategory.VISUALS,
        description = "Компас-стрелка и маркер к заданной точке мира"
)
public final class Gps extends Module {

    private static final float ARROW_LENGTH_RATIO = 1.6f;
    private static final int SCREEN_MARGIN = 24;

    private volatile BlockPos target;

    private final NumberValue<Float> arrowSize = num("arrow_size", 16.0f, 8.0f, 32.0f);
    private final NumberValue<Float> markerSize = num("marker_size", 16.0f, 8.0f, 32.0f);
    private final BooleanValue showDistance = bool("show_distance", true);
    private final BooleanValue showLocationIcon = bool("show_location_icon", true);
    private final RGBAColorValue arrowColor = color("color", "#FF55C8FF");
    private final BooleanValue autoDelete = bool("auto_delete", false);
    private final NumberValue<Float> autoDeleteDistance = visibleWhen(num("auto_delete_distance", 5.0f, 1.0f, 10.0f), autoDelete::get);

    public void setTarget(BlockPos pos) {
        this.target = pos;
    }

    public void clearTarget() {
        this.target = null;
    }

    public BlockPos getTarget() {
        return target;
    }

    @Override
    public void onDisable() {
        target = null;
    }

    @Override
    public void onTick() {
        BlockPos current = target;
        if (current == null || !autoDelete.get()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        double dx = (current.getX() + 0.5) - mc.player.getX();
        double dz = (current.getZ() + 0.5) - mc.player.getZ();
        if (GpsBearing.horizontalDistance(dx, dz) <= autoDeleteDistance.get()) {
            target = null;
        }
    }

    @Override
    public HudPhase getHudPhase() {
        return HudPhase.FIRST;
    }

    @Override
    public void onRenderHudEngine(Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx, float tickDelta) {
        BlockPos current = target;
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || current == null || mc.player == null || mc.level == null || mc.getWindow() == null) return;

        double dx = (current.getX() + 0.5) - mc.player.getX();
        double dz = (current.getZ() + 0.5) - mc.player.getZ();
        float bearing = GpsBearing.bearingDegrees(dx, dz);
        float playerYaw = mc.player.getViewYRot(tickDelta);
        float rotation = GpsBearing.arrowRotationDegrees(bearing, playerYaw);
        double distance = GpsBearing.horizontalDistance(dx, dz);
        int argb = arrowColor.getArgb();

        float centerX = mc.getWindow().getGuiScaledWidth() / 2.0f;
        float anchorY = mc.getWindow().getGuiScaledHeight() / 4.0f;
        drawArrow(renderer, centerX, anchorY, rotation, argb);

        if (showDistance.get()) {
            drawDistanceLabel(textRenderer, centerX, anchorY + arrowSize.get() + 4.0f, distance, argb);
        }

        if (showLocationIcon.get()) {
            drawLocationIconIfVisible(renderer, textRenderer, current, tickDelta, distance, argb, mc);
        }
    }

    private void drawArrow(Renderer2D renderer, float anchorX, float anchorY, float rotationDeg, int argb) {
        float size = arrowSize.get();
        float half = size / 2.0f;
        float length = size * ARROW_LENGTH_RATIO / 2.0f;

        double rad = Math.toRadians(rotationDeg);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        // Локальные вершины треугольника (нос вверх, назад по X): поворачиваем
        // вокруг якоря на rotationDeg и переносим в экранные координаты.
        double[] points = new double[]{
                rotate(0, -length, cos, sin, anchorX, anchorY, true),
                rotate(0, -length, cos, sin, anchorX, anchorY, false),
                rotate(-half, half, cos, sin, anchorX, anchorY, true),
                rotate(-half, half, cos, sin, anchorX, anchorY, false),
                rotate(half, half, cos, sin, anchorX, anchorY, true),
                rotate(half, half, cos, sin, anchorX, anchorY, false),
        };
        renderer.polygon(points, 3, argb);
    }

    private static double rotate(double lx, double ly, double cos, double sin, double anchorX, double anchorY, boolean wantX) {
        double rx = lx * cos - ly * sin;
        double ry = lx * sin + ly * cos;
        return wantX ? anchorX + rx : anchorY + ry;
    }

    private void drawDistanceLabel(TextRenderer textRenderer, float centerX, float y, double distance, int argb) {
        String text = String.format(Locale.ROOT, "%.1fm", distance);
        boolean wasBuilding = textRenderer.isBuilding();
        if (!wasBuilding) textRenderer.begin();
        double width = textRenderer.getWidth(text, true);
        textRenderer.render(text, centerX - width / 2.0, y, new RenderColor(argb), true);
        if (!wasBuilding) textRenderer.end();
    }

    private void drawLocationIconIfVisible(Renderer2D renderer, TextRenderer textRenderer, BlockPos pos,
                                           float tickDelta, double distance, int argb, Minecraft mc) {
        Vec3 world = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        Vec3 screen = ScreenProjection.worldToScreen(world, tickDelta);
        if (screen == null || screen.z < 0.0 || screen.z > 1.0) return;

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        if (screen.x < -SCREEN_MARGIN || screen.x > width + SCREEN_MARGIN
                || screen.y < -SCREEN_MARGIN || screen.y > height + SCREEN_MARGIN) return;

        float radius = markerSize.get() / 2.0f;
        renderer.circle(screen.x, screen.y, radius, argb);

        if (showDistance.get()) {
            drawDistanceLabel(textRenderer, (float) screen.x, (float) screen.y + radius + 4.0f, distance, argb);
        }
    }
}
