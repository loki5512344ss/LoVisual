/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemCooldowns;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.features.module.Modules;
import dev.loki.lovisual.features.module.modules.combat.TPSSync;
import dev.loki.lovisual.mixins.accessors.ItemCooldownEntryAccessor;

import java.util.Iterator;
import java.util.Map;

@Mixin(ItemCooldowns.class)
public abstract class ItemCooldownsMixin {

    @Shadow
    private int tickCount;
    @Shadow
    @Final
    private Map<Identifier, ?> cooldowns;
    @Unique
    private float lovisual$timerRemainder = 0f;

    @Shadow
    protected abstract void onCooldownEnded(Identifier groupId);

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void lovisual$timerCompensate(CallbackInfo ci) {
        // Timer module removed; scale only by server TPS when active.
        float serverDelta = 1.0f;
        TPSSync tps = Modules.get(TPSSync.class);
        if (tps != null && tps.isEnabled()) {
            serverDelta = tps.getServerTickDelta();
        }

        if (serverDelta > 1.0f) serverDelta = 1.0f;
        if (serverDelta < 0.0f) serverDelta = 0.0f;

        if (Math.abs(serverDelta - 1.0f) < 0.0001f) {
            return;
        }

        lovisual$timerRemainder += serverDelta;
        int inc = (int) lovisual$timerRemainder;
        if (inc <= 0) {
            ci.cancel();
            return;
        }
        lovisual$timerRemainder -= inc;

        tickCount += inc;
        if (!cooldowns.isEmpty()) {
            @SuppressWarnings("unchecked")
            Iterator<Map.Entry<Identifier, Object>> iterator =
                    ((Map<Identifier, Object>) cooldowns).entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Identifier, Object> entry = iterator.next();
                Object value = entry.getValue();
                if (value instanceof ItemCooldownEntryAccessor accessor && accessor.getEndTick() <= tickCount) {
                    iterator.remove();
                    onCooldownEnded(entry.getKey());
                }
            }
        }

        ci.cancel();
    }
}
