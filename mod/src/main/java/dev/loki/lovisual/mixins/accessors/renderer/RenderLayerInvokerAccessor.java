package dev.loki.lovisual.mixins.accessors.renderer;

import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderType.class)
public interface RenderLayerInvokerAccessor {

    @Invoker("create")
    static RenderType callOf(String name, RenderSetup setup) {
        throw new AssertionError();
    }
}

