/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.render.entity;

import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.scene.EndCrystal;
import net.minecraft.client.model.object.crystal.EndCrystalModel;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Скрытие частей модели энд-кристалла по настройкам {@link EndCrystal}.
 * {@code outerGlass}, {@code innerGlass} и {@code cube} — вложенные друг в
 * друга {@link net.minecraft.client.model.geom.ModelPart}, поэтому вместо
 * {@code visible} (которое обрезало бы и дочерние части) используется
 * {@code skipDraw} — он прячет только собственную геометрию части, не
 * затрагивая рендер детей.
 */
@Mixin(EndCrystalModel.class)
public abstract class EndCrystalModelMixin {

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void lovisual$applyPartVisibility(EndCrystalRenderState state, CallbackInfo ci) {
        EndCrystalModel self = (EndCrystalModel) (Object) this;
        EndCrystal module = Modules.get(EndCrystal.class);
        if (module == null || !module.isEnabled()) return;

        self.outerGlass.skipDraw = !module.outerGlassVisible.get();
        self.innerGlass.skipDraw = !module.innerGlassVisible.get();
        self.cube.skipDraw = !module.cubeVisible.get();
    }
}
