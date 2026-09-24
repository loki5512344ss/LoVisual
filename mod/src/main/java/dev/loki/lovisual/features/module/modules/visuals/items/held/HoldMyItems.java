package dev.loki.lovisual.features.module.modules.visuals.items.held;

import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.util.animation.EaseCurves;
import dev.loki.lovisual.util.animation.hand.state.HandAnimState;
import dev.loki.lovisual.render.helpers.util.TickDelta;
import dev.loki.lovisual.features.module.modules.visuals.scene.visual.Chams;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;

/**
 * Плавная анимация предмета в руке: ease-out-back equip, sway по обзору,
 * боб при ходьбе, дрейф при использовании. Состояние шагает ровно раз за
 * кадр в {@link #onFrame} — вне рендер-пути, поэтому повторный рендер рук
 * (маска Chams) всегда видит те же значения, что и основной проход.
 */
@ModuleInfo(
        id = "holdmyitems",
        displayName = "Item Holder",
        aliases = {"itemholder"},
        category = ModuleCategory.VISUALS,
        description = "Плавная анимация удержания предмета в руке вместо линейного ваниллового equip"
)
public final class HoldMyItems extends Module {

    private final HandAnimState handAnim = new HandAnimState();

    public final NumberValue<Float> swayIntensity = num("sway_intensity", 1.0f, 0.0f, 2.0f);
    public final NumberValue<Float> easeStrength = num("ease_strength", 1.6f, 0.5f, 3.5f);
    public final BooleanValue offhandToo = bool("offhand", true);
    public final BooleanValue walkBob = bool("walk_bob", true);
    public final NumberValue<Float> bobStrength = visibleWhen(num("bob_strength", 1.0f, 0.0f, 2.0f), walkBob::get);
    public final BooleanValue useDrift = bool("use_drift", true);
    public final NumberValue<Float> driftStrength = visibleWhen(num("drift_strength", 1.0f, 0.0f, 2.0f), useDrift::get);
    public final BooleanValue joinAppear = bool("join_appear", true);

    public final BooleanValue gripEnabled = bool("grip_enabled", true);
    public final NumberValue<Float> gripExtend = visibleWhen(num("grip_extend", 0.3f, -0.3f, 1.2f), gripEnabled::get);
    public final NumberValue<Float> gripSlide = visibleWhen(num("grip_slide", 0.1f, -0.2f, 0.4f), gripEnabled::get);
    public final NumberValue<Float> gripOut = visibleWhen(num("grip_out", 0.05f, -0.25f, 0.25f), gripEnabled::get);
    public final NumberValue<Float> gripForward = visibleWhen(num("grip_forward", 0.14f, -0.2f, 0.5f), gripEnabled::get);
    public final NumberValue<Float> gripTilt = visibleWhen(num("grip_tilt", 0.0f, -90.0f, 90.0f), gripEnabled::get);
    public final NumberValue<Float> gripTurn = visibleWhen(num("grip_turn", 0.0f, -90.0f, 90.0f), gripEnabled::get);
    public final NumberValue<Float> gripRoll = visibleWhen(num("grip_roll", 0.0f, -180.0f, 180.0f), gripEnabled::get);

    public HandAnimState handAnim() {
        return handAnim;
    }

    @Override
    public void onFrame(float tickDelta) {
        if (!isEnabled()) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        handAnim.beginFrame(
                player,
                Minecraft.getInstance().level,
                player.getViewYRot(tickDelta),
                player.getViewXRot(tickDelta),
                player.isUsingItem() && player.getUsedItemHand() == InteractionHand.MAIN_HAND,
                player.isUsingItem() && player.getUsedItemHand() == InteractionHand.OFF_HAND,
                TickDelta.fixedDeltaTicks()
        );
    }

    @Override
    public void onEnable() {
        handAnim.reset();
    }

    /** Перекладывает линейный equip-прогресс через ease-out-back с настройками модуля. */
    public float shapeEquipProgress(float linear) {
        return EaseCurves.shapeEquipProgress(linear, easeStrength.get(), swayIntensity.get());
    }
}
