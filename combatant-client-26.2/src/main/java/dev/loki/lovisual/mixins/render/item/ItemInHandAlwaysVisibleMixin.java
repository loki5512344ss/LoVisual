/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.render.item;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.loki.lovisual.features.module.Modules;
import dev.loki.lovisual.features.module.modules.visuals.items.AlwaysVisibleHand;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Ваниль прячет руку с предметом, пока игрок использует другой предмет
 * (еда/щит/натянутый лук): evaluateWhichHandsToRender возвращает
 * RENDER_OFF_HAND_ONLY/RENDER_MAIN_HAND_ONLY. Вместо подмены
 * package-private enum переопределяем чтение его флагов в
 * submitHandsWithItems — по настройке модуля AlwaysVisibleHand.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandAlwaysVisibleMixin {

    @ModifyExpressionValue(
            method = "submitHandsWithItems",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer$HandRenderSelection;renderMainHand:Z"
            )
    )
    private boolean lovisual$alwaysRenderMainHand(boolean vanillaDecision) {
        AlwaysVisibleHand module = Modules.get(AlwaysVisibleHand.class);
        return module == null ? vanillaDecision : module.renderMainHand(vanillaDecision);
    }

    @ModifyExpressionValue(
            method = "submitHandsWithItems",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer$HandRenderSelection;renderOffHand:Z"
            )
    )
    private boolean lovisual$alwaysRenderOffHand(boolean vanillaDecision) {
        AlwaysVisibleHand module = Modules.get(AlwaysVisibleHand.class);
        return module == null ? vanillaDecision : module.renderOffHand(vanillaDecision);
    }
}
