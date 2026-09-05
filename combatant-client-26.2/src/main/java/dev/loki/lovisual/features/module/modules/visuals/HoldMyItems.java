/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals;

import dev.loki.lovisual.config.values.BooleanValue;
import dev.loki.lovisual.config.values.NumberValue;
import dev.loki.lovisual.features.module.Module;
import dev.loki.lovisual.features.module.ModuleCategory;
import dev.loki.lovisual.features.module.ModuleInfo;
import dev.loki.lovisual.util.animation.EaseCurves;
import dev.loki.lovisual.util.animation.hand.HandAnimState;
import dev.loki.lovisual.render.helpers.TickDelta;
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
