package dev.loki.lovisual.features.module.modules.visuals.placeanimation;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.EnumSet;

/** One block sliding, tilting and scaling into its place; drawn as a moving block while the mesh hides it. */
final class PlacedBlock {
    /** Animation settings captured at placement time. */
    record Params(int ticks, float slide, float tiltRadians, float startScale) {
    }

    private static final float EPSILON = 1.0E-6f;
    private static final double NEAR_EDGE = 0.25;
    private static final double FAR_EDGE = 0.75;

    private final ClientLevel level;
    private final BlockPos pos;
    private final BlockState state;
    private final boolean rendersModel;
    private final Params params;
    private final Vec3 offset;
    private final float yaw;
    private final float slideY;
    private final float slideZ;
    private final MovingBlockRenderState renderState = new MovingBlockRenderState();
    private int age;

    PlacedBlock(ClientLevel level, BlockPos pos, BlockState state, Player player, InteractionHand hand, Params params) {
        this.level = level;
        this.pos = pos;
        this.state = state;
        this.rendersModel = state.getRenderShape() == RenderShape.MODEL;
        this.params = params;
        this.offset = state.getOffset(pos);

        boolean rightHand = (player.getMainArm() == HumanoidArm.RIGHT) == (hand == InteractionHand.MAIN_HAND);
        Vector3f start = PlaceMotion.slideStart(player.getYRot(), player.getXRot(), rightHand, params.slide());
        float length = start.length();
        Vector3f free = restrictToFreeNeighbours(player, start);
        float freeLength = free.length();
        if (length < EPSILON || freeLength < EPSILON) {
            yaw = 0.0f;
            slideY = 0.0f;
            slideZ = 0.0f;
        } else {
            free.div(freeLength);
            yaw = (float) Math.atan2(free.x, free.z);
            slideY = free.y * length;
            slideZ = (float) (free.z * Math.cos(-yaw) - free.x * Math.sin(-yaw)) * length;
        }

        renderState.blockState = state;
        renderState.blockPos = pos;
        renderState.randomSeedPos = pos;
        renderState.biome = level.getBiome(pos);
        renderState.cardinalLighting = level.cardinalLighting();
        renderState.lightEngine = level.getLightEngine();
    }

    ClientLevel level() {
        return level;
    }

    BlockPos pos() {
        return pos;
    }

    BlockState state() {
        return state;
    }

    boolean rendersModel() {
        return rendersModel;
    }

    void tick() {
        age++;
    }

    boolean finished() {
        return age >= params.ticks();
    }

    boolean expired(int graceTicks) {
        return age >= params.ticks() + graceTicks;
    }

    void submit(PoseStack stack, SubmitNodeCollector collector, Vec3 camera, float partial) {
        stack.pushPose();
        stack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
        apply(stack, partial);
        collector.submitMovingBlock(stack, renderState, 0);
        stack.popPose();
    }

    /** Transforms a pose stack positioned at the block's corner into the animated pose. */
    void apply(PoseStack stack, float partial) {
        float progress = PlaceMotion.progress(age, partial, params.ticks());
        float remaining = PlaceMotion.slideRemaining(progress);
        float scale = PlaceMotion.scale(params.startScale(), progress);
        float tilt = PlaceMotion.tilt(progress, params.tiltRadians());

        stack.translate(0.5 + offset.x, 0.5 + offset.y, 0.5 + offset.z);
        stack.mulPose(new Quaternionf().rotationY(yaw));
        stack.translate(0.0f, slideY * remaining, slideZ * remaining);
        stack.translate(0.0f, -0.5f, 0.0f);
        stack.mulPose(new Quaternionf().rotationX(tilt));
        stack.translate(0.0f, 0.5f, 0.0f);
        stack.scale(scale, scale, scale);
        stack.mulPose(new Quaternionf().rotationY(-yaw));
        stack.translate(-0.5 - offset.x, -0.5 - offset.y, -0.5 - offset.z);
    }

    /** Keeps the slide from pushing the block through a solid neighbour on the side it would move towards. */
    private Vector3f restrictToFreeNeighbours(Player player, Vector3f start) {
        EnumSet<Direction> blocked = EnumSet.noneOf(Direction.class);
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.values()) {
            probe.setWithOffset(pos, direction);
            VoxelShape shape = level.getBlockState(probe).getCollisionShape(level, probe);
            if (shape.isEmpty()) continue;
            Direction.Axis axis = direction.getAxis();
            boolean touches = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE
                    ? shape.min(axis) <= NEAR_EDGE
                    : shape.max(axis) >= FAR_EDGE;
            if (touches) blocked.add(direction);
        }
        Vector3f free = PlaceMotion.restrict(start, blocked);
        if (free.lengthSquared() >= EPSILON || blocked.isEmpty()) return free;
        for (Direction direction : Direction.orderedByNearest(player)) {
            if (!blocked.contains(direction)) return new Vector3f(direction.getStepX(), direction.getStepY(), direction.getStepZ());
        }
        return free;
    }
}
