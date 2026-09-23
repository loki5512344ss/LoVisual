package dev.loki.lovisual.features.module.modules.combat.prediction;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.config.values.primitive.BooleanMapValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.modules.combat.predictions.PredictionsModel.TimerPlate;
import dev.loki.lovisual.features.module.modules.combat.predictions.PredictionsModel.TrajectoryResult;
import dev.loki.lovisual.features.module.phase.HudPhase;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.helpers.clip.ScreenProjection;
import dev.loki.lovisual.util.target.TargetManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ModuleInfo(id = "predictions", displayName = "Predictions", category = ModuleCategory.COMBAT)
public class Predictions extends Module {
    private static final String SETTING_LINE_COLOR = "line_color";
    private static final String SETTING_PEARL_CUSTOM_COLOR = "pearl_custom_color";
    private static final String SETTING_PEARL_COLOR = "pearl_color";
    private static final String SETTING_MAX_SIM_TICKS = "max_sim_ticks";
    private static final String SETTING_PEARL_MAX_SIM_TICKS = "pearl_max_ticks";
    private static final String SETTING_MAX_PROJECTILES = "max_projectiles";
    private static final String SETTING_PEARL_MAX_PROJECTILES = "pearl_max_projectiles";
    private static final String SETTING_SHOW_TIMER_PLATE = "show_plate";
    private static final String SETTING_AIM_TRAJECTORY = "aim_trajectory";
    private static final String SETTING_AIM_INDICATOR = "aim_indicator";
    private static final String SETTING_TARGETHUD_ON_AIM = "targethud_on_aim";
    private static final String SETTING_GLOW_WALLS = "glow_walls";
    private static final String SETTING_TNT_TIMER = "tnt_timer";
    private static final String SETTING_TNT_COLOR = "tnt_color";
    private static final String SETTING_PROJECTILES = "projectiles";

    private final RGBColorValue lineColorValue = colorNoAlpha("pred_line_color", SETTING_LINE_COLOR, "#11C7C7");
    private final BooleanValue pearlColorToggle = bool("pred_pearl_custom_color", SETTING_PEARL_CUSTOM_COLOR, false);
    private final RGBColorValue pearlColorValue = visibleWhen(colorNoAlpha("pred_pearl_color", SETTING_PEARL_COLOR, "#C68CFF"), pearlColorToggle::get);
    private final NumberValue<Integer> maxSimTicks = num("pred_max_ticks", SETTING_MAX_SIM_TICKS, 120, 20, 600);
    private final NumberValue<Integer> pearlMaxSimTicks = num("pred_pearl_max_ticks", SETTING_PEARL_MAX_SIM_TICKS, 160, 20, 1000);
    private final NumberValue<Integer> maxProjectiles = num("pred_max_projectiles", SETTING_MAX_PROJECTILES, 48, 1, 256);
    private final NumberValue<Integer> pearlMaxProjectiles = num("pred_pearl_max_projectiles", SETTING_PEARL_MAX_PROJECTILES, 12, 1, 128);
    private final BooleanValue showTimerPlateValue = bool("pred_show_timer_plate", SETTING_SHOW_TIMER_PLATE, true);
    private final BooleanValue aimTrajectoryValue = bool("pred_aim_trajectory", SETTING_AIM_TRAJECTORY, true);
    private final BooleanValue aimIndicatorValue = bool("pred_aim_indicator", SETTING_AIM_INDICATOR, false);
    private final BooleanValue targetHudOnAimValue = bool("pred_targethud_on_aim", SETTING_TARGETHUD_ON_AIM, false);
    private final BooleanValue glowWallsValue = bool("pred_glow_walls", SETTING_GLOW_WALLS, false);
    private final BooleanValue tntTimerValue = bool("pred_tnt_timer", SETTING_TNT_TIMER, true);
    private final RGBColorValue tntColorValue = visibleWhen(colorNoAlpha("pred_tnt_color", SETTING_TNT_COLOR, "#FF5555"), tntTimerValue::get);
    private final BooleanMapValue kindToggles = group("pred_kind_toggles", SETTING_PROJECTILES, new java.util.LinkedHashMap<>() {{
        put("arrow", true); put("trident", true); put("fireball", true); put("wind_charge", true);
        put("firework", true); put("snowball", true); put("egg", true);
        put("potion_splash", true); put("potion_lingering", true);
    }});

    private ItemStack tntTimerIcon = ItemStack.EMPTY;
    private final List<TimerPlate> timerPlates = new ArrayList<>();

    @Override public WorldPhase getWorldPhase() { return WorldPhase.END_MAIN; }
    @Override public HudPhase getHudPhase() { return HudPhase.BEFORE_MISC_OVERLAYS; }
    @Override public void onDisable() { TargetManager.setPredictionTarget(null); }
    @Override public void onRenderWorld(PoseStack matrices, SubmitNodeCollector consumers, float tickDelta) {}

    @Override
    public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.level == null) { TargetManager.setPredictionTarget(null); return; }
        List<TrajectoryResult> results = PredictionsProjectileLogic.simulateProjectiles(this, tickDelta);
        PredictionsTargetLogic.updatePredictionTarget(this, tickDelta);
        int baseColor = lineColorValue.getArgb();
        if (aimTrajectoryValue.get()) PredictionsWorldRenderer.renderAimTrajectoryGlow(this, renderer, tickDelta, baseColor);
        if (aimIndicatorValue.get()) PredictionsWorldRenderer.renderAimIndicatorWorld(this, renderer, tickDelta);
        if (results.isEmpty()) return;
        PredictionsWorldRenderer.renderLineTrajectories(this, renderer, results, baseColor);
        PredictionsWorldRenderer.renderImpactEffects(this, renderer, results, baseColor, tickDelta);
    }

    @Override
    public void onRenderHudEngine(Renderer2D renderer, TextRenderer textRenderer, net.minecraft.client.gui.GuiGraphicsExtractor ctx, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.level == null) return;
        boolean showPlate = showTimerPlateValue.get();
        timerPlates.clear();
        List<TrajectoryResult> results = showPlate ? PredictionsProjectileLogic.simulateProjectiles(this, tickDelta) : Collections.emptyList();
        if (showPlate && !results.isEmpty()) {
            for (TrajectoryResult res : results) {
                if (res.points().isEmpty()) continue;
                Vec3 end = res.points().getLast();
                Vec3 screen = ScreenProjection.worldToScreen(end, tickDelta);
                if (screen == null) continue;
                PredictionsHudRenderer.drawTimerPlateForResult(this, renderer, textRenderer, res, (float) screen.x, (float) screen.y);
            }
        }
        if (tntTimerValue.get()) PredictionsHudRenderer.drawTntTimers(this, renderer, textRenderer, tickDelta);
    }

    @Override
    public void onRenderHudEngineForeground(Renderer2D renderer, TextRenderer textRenderer, net.minecraft.client.gui.GuiGraphicsExtractor ctx, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.level == null || timerPlates.isEmpty()) return;
        int seed = 0;
        List<TimerPlate> plates = new ArrayList<>(timerPlates);
        for (TimerPlate plate : plates) {
            if (plate.iconStack().isEmpty()) continue;
            renderer.item(plate.iconStack(), plate.iconX(), plate.iconY(), plate.iconScale(), seed++, Renderer2D.ITEM_OVERLAY_NONE, null);
        }
        if (!plates.isEmpty()) {
            TextRenderer tr = Fonts.renderer("OnestMedium", FontInfo.Type.Regular, textRenderer);
            tr.begin(0.68f);
            for (TimerPlate plate : plates) tr.render(plate.text(), plate.textX(), plate.textY(), new RenderColor(0xFFFFFFFF), false);
            tr.end();
        }
    }

    public NumberValue<Integer> getMaxSimTicks() { return maxSimTicks; }
    public NumberValue<Integer> getPearlMaxSimTicks() { return pearlMaxSimTicks; }
    public NumberValue<Integer> getMaxProjectiles() { return maxProjectiles; }
    public NumberValue<Integer> getPearlMaxProjectiles() { return pearlMaxProjectiles; }
    public BooleanValue getPearlColorToggle() { return pearlColorToggle; }
    public RGBColorValue getPearlColorValue() { return pearlColorValue; }
    public BooleanMapValue getKindToggles() { return kindToggles; }
    public BooleanValue getGlowWallsValue() { return glowWallsValue; }
    public RGBColorValue getLineColorValue() { return lineColorValue; }
    public BooleanValue getTargetHudOnAimValue() { return targetHudOnAimValue; }
    public RGBColorValue getTntColorValue() { return tntColorValue; }
    public List<TimerPlate> getTimerPlates() { return timerPlates; }
    public ItemStack getTntTimerIcon() {
        if (tntTimerIcon.isEmpty()) tntTimerIcon = new ItemStack(Items.TNT);
        return tntTimerIcon;
    }
}
