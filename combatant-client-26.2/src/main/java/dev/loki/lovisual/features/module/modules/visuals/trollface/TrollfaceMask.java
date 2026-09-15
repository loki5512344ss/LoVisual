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
import dev.loki.lovisual.features.relations.PlayerRelations;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.texture.TextureStorage;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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

        for (Player player : mc.level.players()) {
            if (!shouldMask(player, mc.player, targetMode)) continue;
            renderMask(renderer, player, tickDelta, texId);
        }
    }

    private boolean shouldMask(Player candidate, Player self, String targetMode) {
        if (!candidate.isAlive()) return false;
        return switch (targetMode) {
            case "Себя" -> candidate == self;
            case "Друзей" -> candidate != self
                    && candidate instanceof AbstractClientPlayer
                    && PlayerRelations.get().isFriend(candidate.getGameProfile().name());
            case "Всех" -> true;
            default -> candidate == self;
        };
    }

    // Маска рисуется в плоскости лица цели (yaw/pitch головы), а не биллбордом к камере:
    // биллборд к камере вырождается для себя от первого лица (камера почти совпадает с головой
    // по вертикали -> вектор view~(0,1,0) -> right=view×up=0 -> плоскость ложится горизонтально
    // и не видна). Ориентация по взгляду цели работает одинаково для себя/друзей/всех.
    private void renderMask(Renderer3D renderer, LivingEntity renderTarget, float tickDelta, Identifier texId) {
        double headX = renderTarget.getX();
        double headY = renderTarget.getY() + renderTarget.getBbHeight() * 0.72;
        double headZ = renderTarget.getZ();
        Vec3 headPos = new Vec3(headX, headY, headZ);

        Vec3 forward = renderTarget.getViewVector(tickDelta);
        if (forward.lengthSqr() < 1.0E-8) return;
        forward = forward.normalize();

        Vec3 worldUp = new Vec3(0, 1, 0);
        Vec3 right = forward.cross(worldUp).normalize();
        if (right.lengthSqr() < 1.0E-8) {
            right = new Vec3(1, 0, 0);
        }
        Vec3 actualUp = right.cross(forward).normalize();

        // Маска висит чуть впереди лица вдоль направления взгляда, иначе z-fight с головой.
        headPos = headPos.add(forward.scale(0.35));

        float s = size.get();
        float alpha = this.alpha.get();
        int r = 255, g = 255, b = 255;
        int a = (int) (alpha * 255);

        Vec3 p1 = headPos.add(right.scale(-s)).add(actualUp.scale(-s * 0.75));
        Vec3 p2 = headPos.add(right.scale(s)).add(actualUp.scale(-s * 0.75));
        Vec3 p3 = headPos.add(right.scale(s)).add(actualUp.scale(s * 0.75));
        Vec3 p4 = headPos.add(right.scale(-s)).add(actualUp.scale(s * 0.75));

        MeshBuilder mesh = renderer.batchTextured(LoVisualRenderPipelines.WORLD_TEXTURED, texId, Renderer3D.DepthMode.NONE);
        if (mesh == null) return;

        mesh.ensureQuadCapacity();
        int i1 = mesh.vec3(p1.x, p1.y, p1.z).vec2(1, 1).color(r, g, b, a).next();
        int i2 = mesh.vec3(p2.x, p2.y, p2.z).vec2(0, 1).color(r, g, b, a).next();
        int i3 = mesh.vec3(p3.x, p3.y, p3.z).vec2(0, 0).color(r, g, b, a).next();
        int i4 = mesh.vec3(p4.x, p4.y, p4.z).vec2(1, 0).color(r, g, b, a).next();
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
