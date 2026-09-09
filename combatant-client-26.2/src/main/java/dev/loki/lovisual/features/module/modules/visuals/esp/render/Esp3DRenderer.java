/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals.esp.render;

import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.features.module.modules.visuals.esp.ESP;
import dev.loki.lovisual.features.module.modules.visuals.esp.EspColorUtil;
import dev.loki.lovisual.features.module.modules.visuals.esp.EspEntityCollector;
import dev.loki.lovisual.features.module.modules.visuals.esp.EspModel;
import dev.loki.lovisual.features.module.modules.visuals.esp.shader.EspMeshBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

public final class Esp3DRenderer {

    private Esp3DRenderer() {
    }

    public static void render(ESP esp, Renderer3D renderer, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!esp.isEnabled() || !esp.is3DBox() || mc.level == null || mc.player == null) return;

        Vec3 camPos = mc.gameRenderer.mainCamera().position();
        List<EspModel.RenderEntry> entries = EspEntityCollector.collectPlayerEntries(esp, camPos);
        if (entries.isEmpty()) return;

        float previousLineWidth = RenderState.lineWidth;
        MeshBuilder lines;
        MeshBuilder tris;
        try {
            RenderState.lineWidth = esp.box3dLineWidth.get();
            lines = renderer.batch(LoVisualRenderPipelines.WORLD_COLORED_LINES, Renderer3D.DepthMode.NONE);
            tris = renderer.batch(LoVisualRenderPipelines.WORLD_COLORED, Renderer3D.DepthMode.NONE);
        } finally {
            RenderState.lineWidth = previousLineWidth;
        }

        entries.sort(Comparator.comparingDouble(EspModel.RenderEntry::distSq).reversed());
        for (EspModel.RenderEntry entry : entries) {
            Entity entity = entry.entity();
            AABB box = Esp2DRenderer.resolveWorldBox(entity, tickDelta);
            int color = EspEntityCollector.resolve3DColor(entity, entry.color());
            add3DBox(esp, tris, lines, box, color);
        }
    }

    static void add3DBox(ESP esp, MeshBuilder tris, MeshBuilder lines, AABB box, int argb) {
        int rgb = argb & 0x00FFFFFF;
        int fillAlpha = EspColorUtil.alpha255(esp.box3dFillAlpha.get());
        int outlineAlpha = EspColorUtil.alpha255(esp.box3dOutlineAlpha.get());
        float gradientStrength = Math.max(0.0f, Math.min(2.5f, esp.box3dGradientStrength.get()));

        int c000 = EspColorUtil.boxGradientColor(rgb, box, box.minX, box.minY, box.minZ, fillAlpha, gradientStrength);
        int c001 = EspColorUtil.boxGradientColor(rgb, box, box.minX, box.minY, box.maxZ, fillAlpha, gradientStrength);
        int c010 = EspColorUtil.boxGradientColor(rgb, box, box.minX, box.maxY, box.minZ, fillAlpha, gradientStrength);
        int c011 = EspColorUtil.boxGradientColor(rgb, box, box.minX, box.maxY, box.maxZ, fillAlpha, gradientStrength);
        int c100 = EspColorUtil.boxGradientColor(rgb, box, box.maxX, box.minY, box.minZ, fillAlpha, gradientStrength);
        int c101 = EspColorUtil.boxGradientColor(rgb, box, box.maxX, box.minY, box.maxZ, fillAlpha, gradientStrength);
        int c110 = EspColorUtil.boxGradientColor(rgb, box, box.maxX, box.maxY, box.minZ, fillAlpha, gradientStrength);
        int c111 = EspColorUtil.boxGradientColor(rgb, box, box.maxX, box.maxY, box.maxZ, fillAlpha, gradientStrength);

        EspMeshBuilder.addFilledBox(tris, box, c000, c001, c010, c011, c100, c101, c110, c111);
        EspMeshBuilder.addOutlineBox(lines, box,
                EspColorUtil.withAlpha(c000, outlineAlpha),
                EspColorUtil.withAlpha(c001, outlineAlpha),
                EspColorUtil.withAlpha(c010, outlineAlpha),
                EspColorUtil.withAlpha(c011, outlineAlpha),
                EspColorUtil.withAlpha(c100, outlineAlpha),
                EspColorUtil.withAlpha(c101, outlineAlpha),
                EspColorUtil.withAlpha(c110, outlineAlpha),
                EspColorUtil.withAlpha(c111, outlineAlpha));
    }
}
