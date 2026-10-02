package dev.loki.lovisual.features.module.modules.visuals.placeanimation;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Active place animations plus the set of positions the chunk mesher must skip. The hidden set is read
 * from chunk-compile threads, so it is concurrent; everything else stays on the client thread.
 */
final class PlaceAnimations {
    private static final double MAX_DISTANCE_SQR = 1024.0;
    private static final int REBUILD_GRACE_TICKS = 2;
    private static final int REBUILD_FLAGS = 10;

    private final Map<BlockPos, PlacedBlock> active = new HashMap<>();
    private final Set<BlockPos> hidden = ConcurrentHashMap.newKeySet();

    void start(ClientLevel level, BlockPos pos, BlockState state, Player player, InteractionHand hand,
               PlacedBlock.Params params, boolean animateBlockEntities) {
        boolean model = state.getRenderShape() == RenderShape.MODEL;
        if (!model && !animateBlockEntities) return;
        if (pos.distToCenterSqr(Minecraft.getInstance().gameRenderer.mainCamera().position()) > MAX_DISTANCE_SQR) return;

        BlockPos key = pos.immutable();
        active.put(key, new PlacedBlock(level, key, state, player, hand, params));
        if (model) {
            hidden.add(key);
            rebuild(level, key);
        }
    }

    boolean isHidden(BlockPos pos) {
        return !hidden.isEmpty() && hidden.contains(pos);
    }

    void tick(ClientLevel level) {
        if (active.isEmpty()) return;
        for (Iterator<PlacedBlock> it = active.values().iterator(); it.hasNext(); ) {
            PlacedBlock block = it.next();
            if (block.level() != level) continue;
            if (level.getBlockState(block.pos()) != block.state()) {
                reveal(level, block.pos());
                it.remove();
                continue;
            }
            block.tick();
            if (block.finished()) reveal(level, block.pos());
            if (block.expired(REBUILD_GRACE_TICKS)) it.remove();
        }
    }

    void submit(PoseStack stack, SubmitNodeCollector collector, float partial) {
        if (active.isEmpty()) return;
        Vec3 camera = Minecraft.getInstance().gameRenderer.mainCamera().position();
        for (PlacedBlock block : active.values()) {
            if (block.rendersModel()) block.submit(stack, collector, camera, partial);
        }
    }

    void applyBlockEntity(BlockPos pos, PoseStack stack, float partial) {
        if (active.isEmpty() || pos == null) return;
        PlacedBlock block = active.get(pos);
        if (block != null) block.apply(stack, partial);
    }

    void clear() {
        ClientLevel level = Minecraft.getInstance().level;
        for (PlacedBlock block : active.values()) {
            if (level != null && block.level() == level) reveal(level, block.pos());
        }
        active.clear();
        hidden.clear();
    }

    private void reveal(ClientLevel level, BlockPos pos) {
        if (hidden.remove(pos)) rebuild(level, pos);
    }

    private static void rebuild(ClientLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, REBUILD_FLAGS);
    }
}
