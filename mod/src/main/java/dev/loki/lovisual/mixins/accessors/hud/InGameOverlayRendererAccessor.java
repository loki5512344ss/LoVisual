package dev.loki.lovisual.mixins.accessors.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ScreenEffectRenderer.class)
public interface InGameOverlayRendererAccessor {

    @Invoker("submitFire")
    static void callRenderFireOverlay(PoseStack matrices,
                                      SubmitNodeCollector vertexConsumers,
                                      TextureAtlasSprite sprite) {
        throw new AssertionError();
    }

    @Invoker("submitBlockSprite")
    static void callRenderInWallOverlay(TextureAtlasSprite sprite,
                                        PoseStack matrices,
                                        SubmitNodeCollector vertexConsumers,
                                        int color) {
        throw new AssertionError();
    }
}

