package dev.loki.lovisual.features.module.modules.visuals.effects.projectiletrails;

import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanMapValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.render.engine.animation.AnimatedRenderColors;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.util.time.GameClock;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Затухающие ленты за стрелами, жемчугом, трезубцами и другими снарядами. */
@ModuleInfo(
        id = "projectiletrails",
        displayName = "ProjectileTrails",
        category = ModuleCategory.VISUALS,
        description = "Draws fading trails behind arrows, pearls and other projectiles"
)
public class ProjectileTrails extends Module {

    private final TrailStore store = new TrailStore();
    private final NumberValue<Integer> duration = num("projectileTrailsDuration", "duration", 500, 100, 2000);
    private final NumberValue<Float> width = num("projectileTrailsWidth", "width", 0.15f, 0.05f, 1.5f);
    private final BooleanValue fill = bool("projectileTrailsFill", "fill", true);
    private final ModeValue fillType = visibleWhen(modeSetting("projectileTrailsFillType", "fill_type", "Solid", "Solid", "Glow"), fill::get);
    private final BooleanValue lines = bool("projectileTrailsLines", "lines", true);
    private final NumberValue<Float> lineWidth = visibleWhen(num("projectileTrailsLineWidth", "line_width", 2.0f, 1.0f, 5.0f), lines::get);
    private final BooleanValue depthTest = bool("projectileTrailsDepth", "depth_test", true);
    private final ModeValue colorMode = modeSetting("projectileTrailsColorMode", "color_mode", "Theme",
            "Static", "Rainbow", "LightRainbow", "Sky", "Fade", "DoubleColor", "Analogous", "Theme");
    private final RGBAColorValue color = color("projectileTrailsColor", "color", "#CCFF55FF");
    private final RGBAColorValue color2 = color("projectileTrailsColor2", "color2", "#CC55FFFF");
    private final NumberValue<Integer> colorSpeed = num("projectileTrailsColorSpeed", "color_speed", 18, 2, 54);
    private final BooleanMapValue kinds = group("projectileTrailsKinds", "projectiles", Map.of(
            "arrow", true, "trident", true, "pearl", true, "snowball", true, "egg", true,
            "potion", true, "fireball", true, "wind_charge", true, "firework", false));

    @Override
    public WorldPhase getWorldPhase() {
        return WorldPhase.END_MAIN;
    }

    @Override
    public void onDisable() {
        store.clear();
    }

    @Override
    public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.level == null || mc.gameRenderer.mainCamera() == null) return;
        long now = GameClock.millis();
        long life = duration.get();
        for (Entity e : mc.level.entitiesForRendering()) {
            String kind = TrailStore.kindOf(e);
            if (kind != null && kinds.get(kind) && e.isAlive()) store.sample(e, tickDelta, now, life);
        }
        store.prune(now, life, id -> mc.level.getEntity(id) != null);
        if (store.tracks().isEmpty()) return;
        int phase = Math.round((mc.player == null ? 0 : mc.player.tickCount + tickDelta) * 4.0f);
        AnimatedRenderColors.Mode mode = parse(colorMode.get());
        int primary = color.getArgb();
        int secondary = color2.getArgb();
        int speed = colorSpeed.get();
        List<TrailStore.Track> tracks = new ArrayList<>(store.tracks().values());
        Vec3 cam = mc.gameRenderer.mainCamera().position();
        TrailRibbon.render(renderer, tracks, now, life, width.get(), fill.get(), "Glow".equals(fillType.get()),
                lines.get(), lineWidth.get(), depthTest.get(), cam,
                (i, n) -> AnimatedRenderColors.resolve(mode, speed, phase + Math.round(i / (float) Math.max(1, n - 1) * 180), primary, secondary, true));
    }

    private static AnimatedRenderColors.Mode parse(String mode) {
        return switch (mode) {
            case "Rainbow" -> AnimatedRenderColors.Mode.RAINBOW;
            case "LightRainbow" -> AnimatedRenderColors.Mode.LIGHT_RAINBOW;
            case "Sky" -> AnimatedRenderColors.Mode.SKY;
            case "Fade" -> AnimatedRenderColors.Mode.FADE;
            case "DoubleColor" -> AnimatedRenderColors.Mode.DOUBLE_COLOR;
            case "Analogous" -> AnimatedRenderColors.Mode.ANALOGOUS;
            case "Theme" -> AnimatedRenderColors.Mode.THEME;
            default -> AnimatedRenderColors.Mode.STATIC;
        };
    }
}
