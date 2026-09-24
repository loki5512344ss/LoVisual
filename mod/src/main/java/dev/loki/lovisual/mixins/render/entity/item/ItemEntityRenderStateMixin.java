package dev.loki.lovisual.mixins.render.entity.item;

import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import dev.loki.lovisual.mixininterface.render.IItemEntityRenderState;

@Mixin(ItemEntityRenderState.class)
public class ItemEntityRenderStateMixin implements IItemEntityRenderState {
    @Unique
    private boolean lovisual$onGround;

    @Override
    public boolean lovisual$isOnGround() {
        return lovisual$onGround;
    }

    @Override
    public void lovisual$setOnGround(boolean onGround) {
        lovisual$onGround = onGround;
    }
}
