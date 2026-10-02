package dev.loki.lovisual.features.module.modules.visuals.placeanimation;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

/**
 * Blocks you place slide in from your hand, tilting and growing into position. The block is hidden from the
 * chunk mesh for the duration and drawn as a moving block instead (hooks: BlockItemPlaceMixin and friends).
 */
@ModuleInfo(
        id = "placeanimation",
        displayName = "Place Animation",
        aliases = {"placeanimation"},
        category = ModuleCategory.VISUALS
)
public class PlaceAnimation extends Module {
    private final NumberValue<Integer> duration = num("placeAnimationDuration", "duration", 3, 1, 12);
    private final NumberValue<Float> slide = num("placeAnimationSlide", "slide", 1.0f, 0.0f, 4.0f);
    private final NumberValue<Float> tilt = num("placeAnimationTilt", "tilt", 6.0f, 0.0f, 45.0f);
    private final NumberValue<Float> startScale = num("placeAnimationStartScale", "start_scale", 1.0f, 0.1f, 1.0f);
    private final BooleanValue blockEntities = bool("placeAnimationBlockEntities", "block_entities", false);

    private final PlaceAnimations animations = new PlaceAnimations();

    @Override
    public void onTick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) animations.tick(level);
    }

    @Override
    public void onDisable() {
        animations.clear();
    }

    /** Called when the local player has just placed {@code state} at {@code pos}. */
    public void onBlockPlaced(ClientLevel level, BlockPos pos, BlockState state, Player player, InteractionHand hand) {
        if (!isEnabled() || player == null || !isAnimatable(level, pos, state)) return;
        PlacedBlock.Params params = new PlacedBlock.Params(duration.get(), slide.get(),
                (float) Math.toRadians(tilt.get()), startScale.get());
        animations.start(level, pos, state, player, hand, params, blockEntities.get());
    }

    public boolean isHidden(BlockPos pos) {
        return animations.isHidden(pos);
    }

    public void submit(PoseStack stack, SubmitNodeCollector collector, float partial) {
        if (isEnabled()) animations.submit(stack, collector, partial);
    }

    public void applyBlockEntity(BlockPos pos, PoseStack stack, float partial) {
        if (isEnabled()) animations.applyBlockEntity(pos, stack, partial);
    }

    /** Multi-block structures and non-solid blocks animate badly, so they are left alone. */
    private static boolean isAnimatable(ClientLevel level, BlockPos pos, BlockState state) {
        if (state.is(BlockTags.BEDS) || state.getBlock() instanceof DoorBlock || state.getBlock() instanceof DoublePlantBlock) {
            return false;
        }
        if (state.hasProperty(ChestBlock.TYPE) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) return false;
        return !state.getCollisionShape(level, pos).isEmpty();
    }
}
