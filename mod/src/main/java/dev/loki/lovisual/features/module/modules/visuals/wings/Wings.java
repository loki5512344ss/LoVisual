package dev.loki.lovisual.features.module.modules.visuals.wings;

import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.features.relations.PlayerRelations;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

/**
 * Cosmetic wings on player backs (ported from the WiNGS reference): 8 silhouette types,
 * pose-driven flapping (idle/sneak/glide), friends/all-players visibility. Geometry data
 * lives in {@link WingShapes}, mesh building in {@link WingsRenderer}.
 */
@ModuleInfo(
        id = "wings",
        displayName = "Wings",
        category = ModuleCategory.ACCESSORIES,
        description = "Крылья на спине игрока: 8 типов формы, анимация взмахов"
)
public final class Wings extends Module {

    private final ModeValue wingType = modeSetting(
            "wingsType", "type", "Angelic", WingShapes.TYPES);
    private final ModeValue target = modeSetting(
            "wingsTarget", "target", "Self", "Self", "Friends", "All Players");
    private final NumberValue<Float> scale =
            num("wingsScale", "scale", 1.0f, 0.3f, 3.0f);
    private final BooleanValue flapping =
            bool("wingsFlapping", "flapping", true);
    private final NumberValue<Float> flapStrength =
            visibleWhen(num("wingsFlapStrength", "flap_strength", 30.0f, 5.0f, 60.0f), flapping::get);
    private final NumberValue<Float> flapSpeed =
            visibleWhen(num("wingsFlapSpeed", "flap_speed", 3.0f, 0.5f, 8.0f), flapping::get);
    private final BooleanValue throughWalls =
            bool("wingsThroughWalls", "through_walls", false);
    private final BooleanValue glow = bool("wingsGlow", "glow", true);
    private final BooleanValue outline = bool("wingsOutline", "outline", true);
    private final BooleanValue ribs = bool("wingsRibs", "ribs", true);
    private final RGBAColorValue color =
            color("wingsColor", "color", "#FFFFFFDC");

    private float selfYaw;
    private boolean selfYawReady;

    @Override
    public WorldPhase getWorldPhase() {
        return WorldPhase.END_MAIN;
    }

    @Override
    public void onDisable() {
        selfYawReady = false;
    }

    @Override
    public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.level == null || mc.player == null || renderer == null) return;

        WingsRenderer.Style style = new WingsRenderer.Style(
                wingType.get(), scale.get(), flapping.get(), flapStrength.get(), flapSpeed.get(),
                color.getArgb(), glow.get(), outline.get(), ribs.get(), !throughWalls.get());

        if (showsSelf() && !mc.options.getCameraType().isFirstPerson()
                && mc.player.isAlive() && !hasElytra(mc.player)) {
            renderOne(renderer, mc.player, tickDelta, style, true);
        }
        if (showsOthers()) {
            for (Player player : mc.level.players()) {
                if (player == mc.player || !player.isAlive() || hasElytra(player)) continue;
                if (!showsOthers(player)) continue;
                renderOne(renderer, player, tickDelta, style, false);
            }
        }
    }

    private void renderOne(Renderer3D renderer, Player player, float tickDelta,
                           WingsRenderer.Style style, boolean isSelf) {
        float targetYaw = Mth.rotLerp(tickDelta, player.yBodyRotO, player.yBodyRot);
        float bodyYaw;
        if (isSelf) {
            if (!selfYawReady || player.tickCount < 2) {
                selfYaw = targetYaw;
                selfYawReady = true;
            } else {
                selfYaw += Mth.clamp(Mth.wrapDegrees(targetYaw - selfYaw), -14f, 14f);
            }
            bodyYaw = selfYaw;
        } else {
            bodyYaw = targetYaw;
        }
        WingsRenderer.render(renderer, player, tickDelta, bodyYaw, style);
    }

    private boolean showsSelf() {
        String t = target.get();
        return "Self".equals(t) || "All Players".equals(t);
    }

    private boolean showsOthers() {
        return "Friends".equals(target.get()) || "All Players".equals(target.get());
    }

    private boolean showsOthers(Player player) {
        String t = target.get();
        if ("All Players".equals(t)) return true;
        if ("Friends".equals(t)) {
            return PlayerRelations.get().isFriend(player.getGameProfile().name());
        }
        return false;
    }

    private static boolean hasElytra(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
    }
}
