/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.loki.lovisual.features.module.Modules;
import dev.loki.lovisual.features.module.modules.combat.PvpCooldowns;
import dev.loki.lovisual.features.module.modules.visuals.FovControl;
import dev.loki.lovisual.util.pvp.CooldownRegistry;
import dev.loki.lovisual.util.pvp.PvpTargetState;
import dev.loki.lovisual.util.pvp.opponents.OpponentCooldownManager;

@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerMixin {


    @Unique
    private boolean lovisual$wasUsing = false;

    @Inject(method = "getFieldOfViewModifier", at = @At("HEAD"), cancellable = true)
    private void lovisual$overrideFovMultiplier(boolean changingFov, float fovEffectScale,
                                                 CallbackInfoReturnable<Float> cir) {
        FovControl module = Modules.get(FovControl.class);
        if (module == null || !module.isEnabled()) return;

        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;

        float f = 1.0f;

        if (player.getAbilities().flying && !module.disableFlyingFov()) {
            f *= 1.1f;
        }

        boolean skipSpeed = module.disableSprintFov() && player.isSprinting();
        if (module.disableSpeedFov() && player.hasEffect(MobEffects.SPEED)) skipSpeed = true;
        if (module.disableSlownessFov() && player.hasEffect(MobEffects.SLOWNESS)) skipSpeed = true;

        if (!skipSpeed) {
            float walkSpeed = player.getAbilities().getWalkingSpeed();
            if (walkSpeed != 0.0f) {
                float attr = (float) player.getAttributeValue(Attributes.MOVEMENT_SPEED);
                float speedFactor = attr / walkSpeed;
                f *= (speedFactor + 1.0f) / 2.0f;
            }
        }

        if (player.isUsingItem()) {
            if (player.getUseItem().is(Items.BOW)) {
                if (!module.disableBowFov()) {
                    float use = player.getTicksUsingItem() / 20.0f;
                    use = Math.min(use, 1.0f);
                    f *= 1.0f - (Mth.sqrt(use) * 0.15f);
                }
            } else if (changingFov && player.isScoping()) {
                if (!module.disableSpyglassFov()) {
                    cir.setReturnValue(0.1f);
                    return;
                }
            }
        }

        float mult = Mth.lerp(fovEffectScale, 1.0f, f);

        float expand = Math.max(0.0f, module.getExpandScale());
        float shrink = Math.max(0.0f, module.getShrinkScale());
        if (mult > 1.0f) {
            mult = 1.0f + (mult - 1.0f) * expand;
        } else if (mult < 1.0f) {
            mult = 1.0f - (1.0f - mult) * shrink;
        }

        cir.setReturnValue(mult);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void lovisual$trackEat(CallbackInfo ci) {
        AbstractClientPlayer p = (AbstractClientPlayer) (Object) this;
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || p == mc.player) {
            lovisual$wasUsing = p.isUsingItem();
            return;
        }

        boolean using = p.isUsingItem();

        PvpCooldowns mod = Modules.get(PvpCooldowns.class);
        if (mod == null || !mod.isSystemEnabled() || !PvpTargetState.isTargetInPvp(p.getUUID())) {
            lovisual$wasUsing = using;
            return;
        }

        if (using && !lovisual$wasUsing) {
            ItemStack stack = p.getUseItem();
            if (!stack.isEmpty() && CooldownRegistry.isTracked(stack.getItem())) {
                OpponentCooldownManager.recordUse(p.getUUID(), stack.getItem());
            }
        }

        lovisual$wasUsing = using;
    }
}




