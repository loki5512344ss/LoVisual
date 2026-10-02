package dev.loki.lovisual.features.module.modules.visuals.effects.elytratrails;

import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.features.module.modules.visuals.effects.ribbon.TrackBuffer;
import dev.loki.lovisual.features.module.modules.visuals.effects.ribbon.TrailRibbon;
import dev.loki.lovisual.render.engine.animation.AnimatedRenderColors;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.util.time.GameClock;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Светящиеся шлейфы с концов крыльев элитры в полёте (по мотивам evo ElytraTrails). */
@ModuleInfo(
        id = "elytratrails",
        displayName = "ElytraTrails",
        category = ModuleCategory.VISUALS,
        description = "Glowing trails streaming from elytra wing tips while gliding"
)
public class ElytraTrails extends Module {

    private final WingTrailStore store = new WingTrailStore();

    private final BooleanValue others = bool("elytraTrailsOthers", "others", true);
    private final NumberValue<Integer> duration = num("elytraTrailsDuration", "duration", 700, 150, 3000);
    private final NumberValue<Float> width = num("elytraTrailsWidth", "width", 0.35f, 0.05f, 1.5f);
    private final BooleanValue glow = bool("elytraTrailsGlow", "glow", true);
    private final BooleanValue lines = bool("elytraTrailsLines", "lines", false);
    private final NumberValue<Float> lineWidth = visibleWhen(num("elytraTrailsLineWidth", "line_width", 2.0f, 0.5f, 5.0f), lines::get);
    private final BooleanValue depthTest = bool("elytraTrailsDepth", "depth_test", true);
    private final ModeValue colorMode = modeSetting("elytraTrailsColorMode", "color_mode", "DoubleColor",
            "Static", "Rainbow", "LightRainbow", "Sky", "Fade", "DoubleColor", "Analogous", "Theme");
    private final RGBAColorValue color = color("elytraTrailsColor", "color", "#88FF66CC");
    private final RGBAColorValue color2 = color("elytraTrailsColor2", "color2", "#6688FFFF");
    private final NumberValue<Integer> colorSpeed = num("elytraTrailsColorSpeed", "color_speed", 18, 2, 54);
    private final NumberValue<Integer> colorPhase = num("elytraTrailsColorPhase", "color_phase", 0, 0, 360);

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
        for (Player player : mc.level.players()) {
            if (!others.get() && player != mc.player) continue;
            if (!player.isAlive() || !player.isFallFlying()) continue;
            WingTipLocator.Tips tips = WingTipLocator.tips(player.getEyePosition(tickDelta),
                    player.getViewYRot(tickDelta), player.getViewXRot(tickDelta));
            store.sample(player.getUUID(), WingTrailStore.LEFT, tips.left().x, tips.left().y, tips.left().z, now, life);
            store.sample(player.getUUID(), WingTrailStore.RIGHT, tips.right().x, tips.right().y, tips.right().z, now, life);
        }
        store.prune(now, life);
        List<TrackBuffer> tracks = store.snapshot();
        if (tracks.isEmpty()) return;

        int phase = Math.round((mc.player == null ? 0 : mc.player.tickCount + tickDelta) * 4.0f) + colorPhase.get();
        int primary = color.getArgb();
        int secondary = color2.getArgb();
        int speed = colorSpeed.get();
        AnimatedRenderColors.Mode mode = AnimatedRenderColors.modeOf(colorMode.get());
        Vec3 cam = mc.gameRenderer.mainCamera().position();
        TrailRibbon.render(renderer, tracks, now, life, width.get(), true, glow.get(), lines.get(), lineWidth.get(),
                depthTest.get(), cam,
                (i, n) -> AnimatedRenderColors.resolve(mode, speed,
                        phase + Math.round(i / (float) Math.max(1, n - 1) * 180), primary, secondary, true));
    }
}
