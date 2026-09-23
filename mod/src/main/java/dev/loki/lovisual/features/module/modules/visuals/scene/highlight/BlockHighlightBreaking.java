package dev.loki.lovisual.features.module.modules.visuals.scene;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import dev.loki.lovisual.mixins.accessors.misc.MultiPlayerGameModeAccessor;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;

final class BlockHighlightBreaking {

    void render(Renderer3D renderer,
                BlockHighlightColors colors,
                BlockHighlightGeometry geometry,
                BlockHighlight.BreakAnimation animation,
                boolean self,
                boolean otherPlayers,
                int colorArgb,
                int color2Argb) {
        if (self) {
            renderSelf(renderer, colors, geometry, animation, colorArgb, color2Argb);
        }
        if (otherPlayers) {
            renderOthers(renderer, colors, geometry, animation, colorArgb, color2Argb);
        }
    }

    private void renderSelf(Renderer3D renderer,
                            BlockHighlightColors colors,
                            BlockHighlightGeometry geometry,
                            BlockHighlight.BreakAnimation animation,
                            int colorArgb,
                            int color2Argb) {
        Minecraft mc = Minecraft.getInstance();
        MultiPlayerGameMode interactionManager = mc.gameMode;
        if (interactionManager == null || !interactionManager.isDestroying()) return;
        if (!(interactionManager instanceof MultiPlayerGameModeAccessor accessor)) return;

        BlockPos pos = accessor.lovisual$getCurrentBreakingPos();
        if (pos == null) return;

        float progress = Mth.clamp(accessor.lovisual$getCurrentBreakingProgress(), 0.0f, 1.0f);
        renderInfo(renderer, colors, geometry, animation, pos, progress, colorArgb, color2Argb);
    }

    private void renderOthers(Renderer3D renderer,
                              BlockHighlightColors colors,
                              BlockHighlightGeometry geometry,
                              BlockHighlight.BreakAnimation animation,
                              int colorArgb,
                              int color2Argb) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        var destructionProgress = mc.level.destructionProgress();
        if (destructionProgress == null || destructionProgress.isEmpty()) return;

        destructionProgress.forEach((packedPos, infos) -> {
            if (infos == null || infos.isEmpty()) return;

            BlockDestructionProgress info = infos.last();
            if (mc.player != null && info.getId() == mc.player.getId()) return;

            renderInfo(renderer, colors, geometry, animation, info.getPos(),
                    colors.stageToProgress(info.getProgress()), colorArgb, color2Argb);
        });
    }

    private void renderInfo(Renderer3D renderer,
                            BlockHighlightColors colors,
                            BlockHighlightGeometry geometry,
                            BlockHighlight.BreakAnimation animation,
                            BlockPos pos,
                            float progress,
                            int colorArgb,
                            int color2Argb) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || pos == null) return;

        BlockState state = mc.level.getBlockState(pos);
        if (state.isAir()) return;

        var shape = state.getShape(mc.level, pos, CollisionContext.empty());
        if (shape.isEmpty()) return;

        float clamped = Mth.clamp(progress, 0.0f, 1.0f);
        int argb = colors.breakingArgb(colorArgb, color2Argb, clamped);

        shape.move(pos.getX(), pos.getY(), pos.getZ()).forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            AABB box = animateBox(new AABB(minX, minY, minZ, maxX, maxY, maxZ), animation, clamped);
            if (isRenderable(box)) {
                geometry.addFilledBox(renderer, box, argb);
            }
        });
    }

    private AABB animateBox(AABB box, BlockHighlight.BreakAnimation animation, float progress) {
        double scale = switch (animation) {
            case GROW -> progress;
            case SHRINK -> 1.0 - progress;
            case STATIC -> 1.0;
        };

        scale = Mth.clamp((float) scale, 0.0f, 1.0f);
        if (scale >= 0.999f) {
            return box;
        }

        double cx = (box.minX + box.maxX) * 0.5;
        double cy = (box.minY + box.maxY) * 0.5;
        double cz = (box.minZ + box.maxZ) * 0.5;
        double hx = (box.maxX - box.minX) * 0.5 * scale;
        double hy = (box.maxY - box.minY) * 0.5 * scale;
        double hz = (box.maxZ - box.minZ) * 0.5 * scale;

        return new AABB(cx - hx, cy - hy, cz - hz, cx + hx, cy + hy, cz + hz);
    }

    private boolean isRenderable(AABB box) {
        return box.maxX - box.minX > 1.0e-4
                && box.maxY - box.minY > 1.0e-4
                && box.maxZ - box.minZ > 1.0e-4;
    }
}
