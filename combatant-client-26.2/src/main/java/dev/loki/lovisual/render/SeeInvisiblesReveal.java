/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render;

import dev.loki.lovisual.features.module.Modules;
import dev.loki.lovisual.features.module.modules.visuals.esp.overlay.InvisEsp;
import dev.loki.lovisual.render.iris.IrisRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Логика See Invisibles: невидимый игрок раскрывается — флаг невидимости
 * снимается перед рендером, а render type форсится в полупрозрачный.
 */
public enum SeeInvisiblesReveal {
    ;

    public static <T extends LivingEntity> void reveal(T entity, LivingEntityRenderState state) {
        if (!(entity instanceof Player player) || player == Minecraft.getInstance().player) {
            return;
        }
        InvisEsp module = Modules.get(InvisEsp.class);
        if (module == null || !module.isEnabled() || !entity.isInvisible()) {
            return;
        }
        if (state instanceof SeeInvisiblesState seeInvis) {
            seeInvis.lovisual$setSeeInvisRevealed(true);
        }
        state.isInvisible = false;
    }

    public static RenderType renderTypeFor(RenderType original, Identifier texture) {
        InvisEsp module = Modules.get(InvisEsp.class);
        if (module == null || !module.isEnabled() || IrisRuntime.isRenderingShadowPass()) {
            return original;
        }
        if (!module.isTranslucentReveal()) {
            return original;
        }
        if (original != null && original.hasBlending()) {
            return original;
        }
        return RenderTypes.entityTranslucent(texture, true);
    }
}
