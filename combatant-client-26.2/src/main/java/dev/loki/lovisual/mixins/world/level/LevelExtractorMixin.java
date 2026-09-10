/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.world.level;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.camera.Freecam;
import dev.loki.lovisual.mixins.accessors.world.LevelExtractorAccessor;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin {

    @Inject(method = "extractVisibleEntities", at = @At("TAIL"))
    private void freecam$addLocalPlayer(
            Camera camera,
            Frustum frustum,
            DeltaTracker tickCounter,
            LevelRenderState renderStates,
            CallbackInfo ci
    ) {
        Freecam fc = Modules.get(Freecam.class);
        Minecraft mc = Minecraft.getInstance();

        if (fc == null || !fc.isEnabled()) return;
        if (mc.player == null) return;

        for (EntityRenderState state : renderStates.entityRenderStates) {
            if (state instanceof AvatarRenderState prs && prs.id == mc.player.getId()) {
                return;
            }
        }

        EntityRenderState state = ((LevelExtractorAccessor) this).lovisual$invokeExtractEntity(
                mc.player,
                tickCounter.getGameTimeDeltaPartialTick(false)
        );

        if (state instanceof AvatarRenderState prs) {
            renderStates.entityRenderStates.add(prs);
        }
    }
}
