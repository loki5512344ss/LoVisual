package dev.loki.lovisual.mixins.accessors.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {

    @Invoker("bobHurt")
    void invokeTiltViewWhenHurt(CameraRenderState cameraRenderState, PoseStack matrices);

    @Invoker("bobView")
    void invokeBobView(CameraRenderState cameraRenderState, PoseStack matrices);

    @Invoker("renderItemInHand")
    void invokeRenderHand(CameraRenderState cameraRenderState, float tickDelta, Matrix4fc positionMatrix);

    @Accessor("gameRenderState")
    GameRenderState lovisual$getGameRenderState();

    @Accessor("handAndScreenSubmitNodeStorage")
    SubmitNodeStorage lovisual$getHandAndScreenSubmitNodeStorage();

    @Accessor("mainRenderTarget")
    RenderTarget lovisual$getMainRenderTargetRaw();

    @Accessor("guiRenderer")
    GuiRenderer lovisual$getGuiRenderer();

    @Accessor("fogRenderer")
    FogRenderer lovisual$getFogRenderer();

    @Accessor("spinningEffectTime")
    float lovisual$getNauseaEffectTime();

    @Accessor("spinningEffectSpeed")
    float lovisual$getNauseaEffectSpeed();
}
