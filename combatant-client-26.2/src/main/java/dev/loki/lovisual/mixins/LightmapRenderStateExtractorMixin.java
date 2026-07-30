/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins;

import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.events.Events;
import dev.loki.lovisual.events.impl.LightmapModifyEvent;
import dev.loki.lovisual.render.engine.light.LightmapState;

@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightmapRenderStateExtractorMixin {
    @Inject(method = "extract", at = @At("TAIL"))
    private void combatant$modifyLightmapState(LightmapRenderState state, float tickDelta, CallbackInfo ci) {
        if (state == null) return;

        LightmapModifyEvent event = new LightmapModifyEvent(state);
        Events.BUS.post(event);

        if (event.isModified()) {
            state.needsUpdate = true;
            LightmapState.setAmbient(event.originalBrightness(), state.brightness);
        }
    }
}
