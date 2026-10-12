package dev.loki.lovisual.mixins.optimize.entity.anim;

import dev.loki.lovisual.render.engine.optimize.entity.AnimFreeze;
import dev.loki.lovisual.render.engine.optimize.entity.AnimHolder;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Stage 3 step 4: implements {@link AnimHolder} on {@code Entity} — adds the
 * per-entity bag of frozen animation captures. The field lives as long as
 * the entity; captures hold direct buffers, so long-lived entities keep at
 * most one capture per (model, renderType, sprite) submission slot.
 */
@Mixin(Entity.class)
public abstract class EntityAnimHolderMixin implements AnimHolder {

    @Unique
    private AnimFreeze.Holder lovisual$anim;

    @Override
    public AnimFreeze.Holder lovisual$anim() {
        return lovisual$anim;
    }

    @Override
    public void lovisual$anim(AnimFreeze.Holder holder) {
        lovisual$anim = holder;
    }
}
