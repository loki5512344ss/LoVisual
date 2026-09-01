/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals;

import dev.loki.lovisual.config.values.NumberValue;
import dev.loki.lovisual.config.values.RGBAColorValue;
import dev.loki.lovisual.features.module.Module;
import dev.loki.lovisual.features.module.ModuleCategory;
import dev.loki.lovisual.features.module.ModuleInfo;
import dev.loki.lovisual.features.module.WorldPhase;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

@ModuleInfo(
        id = "invisesp",
        displayName = "InvisESP",
        aliases = {"invis"},
        category = ModuleCategory.VISUALS,
        description = "module.invisesp.description"
)
public final class InvisEsp extends Module {

    private final Minecraft mc = Minecraft.getInstance();

    private final RGBAColorValue outlineColor =
            color("invisesp_color", "color", "#FF00E5FF");
    private final NumberValue<Float> lineWidth =
            num("invisesp_line_width", "line_width", 1.5f, 0.5f, 6.0f);

    {
        setDefaultBind("L");
    }

    @Override
    public WorldPhase getWorldPhase() {
        return WorldPhase.END_MAIN;
    }

    @Override
    public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        if (!isEnabled() || mc.level == null || mc.player == null) return;

        float previousLineWidth = RenderState.lineWidth;
        MeshBuilder lines;
        try {
            RenderState.lineWidth = lineWidth.get();
            lines = renderer.batch(LoVisualRenderPipelines.WORLD_COLORED_LINES, Renderer3D.DepthMode.NONE);
        } finally {
            RenderState.lineWidth = previousLineWidth;
        }

        int color = outlineColor.getArgb();
        for (Player player : mc.level.players()) {
            if (player == mc.player) continue;
            if (!isInvisible(player)) continue;

            AABB box = Esp2DRenderer.resolveWorldBox(player, tickDelta);
            EspMeshBuilder.addOutlineBox(lines, box, color, color, color, color, color, color, color, color);
        }
    }

    private boolean isInvisible(Player player) {
        return player.isInvisible() || player.hasEffect(MobEffects.INVISIBILITY);
    }
}
