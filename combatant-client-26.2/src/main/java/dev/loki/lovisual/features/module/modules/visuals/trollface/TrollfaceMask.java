/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals.trollface;

import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.texture.TextureStorage;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.util.target.TargetManager;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(
        id = "trollface_mask",
        displayName = "Trollface Mask",
        category = ModuleCategory.VISUALS
)
public class TrollfaceMask extends Module {

    private static TrollfaceMask instance;

    public final ModeValue target = mode(
            "trollfaceTarget", "Себя",
            "Себя", "Друзей", "Всех"
    );

    public final ModeValue maskType = mode(
            "trollfaceMaskType", "Style 1",
            "Style 1", "Style 2", "Style 3",
            "Style 4", "Style 5", "Style 6"
    );

    public final NumberValue<Float> size = num(
            "trollfaceSize", "size", 1.0f, 0.3f, 3.0f
    );

    public final NumberValue<Float> alpha = num(
            "trollfaceAlpha", "alpha", 1.0f, 0.1f, 1.0f
    );

    public TrollfaceMask() {
        instance = this;
    }

    public static TrollfaceMask getInstance() {
        return instance;
    }

    @Override
    public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        String targetMode = target.get();
        boolean selfMode = "Себя".equals(targetMode);

        LivingEntity currentTarget = TargetManager.getTarget();
        boolean hasCrosshairTarget = currentTarget != null && currentTarget.isAlive();

        if (!selfMode && !hasCrosshairTarget) return;

        LivingEntity renderTarget;
        boolean shouldRender;

        switch (targetMode) {
            case "Себя" -> {
                renderTarget = mc.player;
                shouldRender = true;
            }
            case "Друзей" -> {
                if (!hasCrosshairTarget) { shouldRender = false; renderTarget = null; break; }
                renderTarget = currentTarget;
                shouldRender = true;
            }
            case "Всех" -> {
                if (!hasCrosshairTarget) { shouldRender = false; renderTarget = null; break; }
                renderTarget = currentTarget;
                shouldRender = true;
            }
            default -> {
                renderTarget = mc.player;
                shouldRender = true;
            }
        }
        if (!shouldRender || renderTarget == null) return;

        Vec3 camPos = mc.gameRenderer.mainCamera().position();
        double headX = renderTarget.getX();
        double headY = renderTarget.getY() + renderTarget.getBbHeight() * 0.72;
        double headZ = renderTarget.getZ();

        Vec3 headPos = new Vec3(headX, headY, headZ);
        Vec3 viewDir = camPos.subtract(headPos).normalize();

        if (viewDir.lengthSqr() < 1.0E-8) return;

        Vec3 upVec = new Vec3(0, 1, 0);
        Vec3 right = viewDir.cross(upVec).normalize();
        if (right.lengthSqr() < 1.0E-8) {
            right = new Vec3(1, 0, 0);
        }
        Vec3 actualUp = right.cross(viewDir).normalize();

        float s = size.get();
        float alpha = this.alpha.get();
        int color = 0xFFFFFFFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (int) (alpha * 255);

        Vec3 p1 = headPos.add(right.scale(-s)).add(actualUp.scale(-s * 0.75));
        Vec3 p2 = headPos.add(right.scale(s)).add(actualUp.scale(-s * 0.75));
        Vec3 p3 = headPos.add(right.scale(s)).add(actualUp.scale(s * 0.75));
        Vec3 p4 = headPos.add(right.scale(-s)).add(actualUp.scale(s * 0.75));

        int textureIndex = maskTypeIndex(maskType.get());
        Identifier texId = switch (textureIndex) {
            case 0 -> TextureStorage.TROLLFACE_1;
            case 1 -> TextureStorage.TROLLFACE_2;
            case 2 -> TextureStorage.TROLLFACE_3;
            case 3 -> TextureStorage.TROLLFACE_4;
            case 4 -> TextureStorage.TROLLFACE_5;
            case 5 -> TextureStorage.TROLLFACE_6;
            default -> TextureStorage.TROLLFACE_1;
        };

        MeshBuilder mesh = renderer.batchTextured(LoVisualRenderPipelines.WORLD_TEXTURED, texId, Renderer3D.DepthMode.NONE);
        if (mesh == null) return;

        mesh.ensureQuadCapacity();
        int i1 = mesh.vec3(p1.x, p1.y, p1.z).vec2(0, 1).color(r, g, b, a).next();
        int i2 = mesh.vec3(p2.x, p2.y, p2.z).vec2(1, 1).color(r, g, b, a).next();
        int i3 = mesh.vec3(p3.x, p3.y, p3.z).vec2(1, 0).color(r, g, b, a).next();
        int i4 = mesh.vec3(p4.x, p4.y, p4.z).vec2(0, 0).color(r, g, b, a).next();
        mesh.quad(i1, i2, i3, i4);
    }

    private static int maskTypeIndex(String maskType) {
        if (maskType == null) return 0;
        return switch (maskType) {
            case "Style 1" -> 0;
            case "Style 2" -> 1;
            case "Style 3" -> 2;
            case "Style 4" -> 3;
            case "Style 5" -> 4;
            case "Style 6" -> 5;
            default -> 0;
        };
    }
}
