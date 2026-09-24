package dev.loki.lovisual.mixininterface.render;

public interface IGuiGraphics {
    /**
     * Runs a task without vanilla UI scaling applied to DrawContext's matrix stack.
     */
    void lovisual$runUnscaled(Runnable task);

    /**
     * Runs a task with an additional 2D transform applied to DrawContext's matrix stack.
     * Order: translate(tx, ty) then rotate(angleRad). Angle in radians.
     */
    void lovisual$withTransform(float tx, float ty, float angleRad, Runnable task);

    void lovisual$drawItemBar(net.minecraft.world.item.ItemStack stack, int x, int y);

    void lovisual$drawCooldownProgress(net.minecraft.world.item.ItemStack stack, int x, int y);

    void lovisual$addDeferredHudMarker(dev.loki.lovisual.render.engine.renderer.Renderer2D.Deferred2DLayer layer);
}

