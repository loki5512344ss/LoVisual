package dev.loki.lovisual.render.engine.optimize.hud;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import java.util.Optional;
import java.util.OptionalDouble;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;
import org.joml.Vector4f;

/**
 * Stage 3 step 6: the HUD is drawn into its own texture at the monitor
 * refresh rate; frames in between only blend that texture in. Crosshair and
 * camera overlays (vignette, pumpkin...) blend with the world, so they stay
 * live every frame.
 *
 * <p>Disabled outright when a foreign HUD cache (axolotlclient, exordium,
 * gnetum) or VulkanMod is loaded — two caches stacked on one HUD break each
 * other. Off by default: the LoVisual deferred-HUD marker path draws its own
 * ranges straight into the main target and does not go through the cache.
 */
public enum HudCache {
    ;

    // the HUD texture holds premultiplied colour: GUI blending onto transparent black
    // leaves colour * alpha; write mask 7 = RGB only, alpha is not written
    private static final RenderPipeline COMPOSITE = RenderPipeline.builder()
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withLocation(Identifier.fromNamespaceAndPath("lovisual", "pipeline/hud_composite"))
            .withVertexShader("core/screenquad")
            .withFragmentShader("core/blit_screen")
            .withBindGroupLayout(BindGroupLayouts.IN_SAMPLER)
            .withColorTargetState(new ColorTargetState(
                    Optional.of(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA), GpuFormat.RGBA8_UNORM, 7))
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .build();

    private static final Vector4f CLEAR = new Vector4f(0, 0, 0, 0);
    private static final boolean FOREIGN_HUD = isModLoaded("axolotlclient")
            || isModLoaded("exordium")
            || isModLoaded("gnetum");
    private static final boolean VULKAN = isModLoaded("vulkanmod");

    private static TextureTarget target;
    // the game's own main target, taken before the HUD: mods (Xaero's minimap) swap
    // their own one in while they draw
    private static RenderTarget main;
    private static volatile boolean enabled;
    private static boolean active;
    private static boolean refresh;
    private static boolean drawing;
    private static long lastRefresh;
    private static volatile boolean suppressLiveParts;

    public static boolean suppressLiveParts() {
        return suppressLiveParts;
    }

    public static void suppressLiveParts(boolean value) {
        suppressLiveParts = value;
    }

    public static void configure(boolean on) {
        enabled = on && !FOREIGN_HUD && !VULKAN;
    }

    public static void reset() {
        enabled = false;
        active = false;
        refresh = false;
        drawing = false;
        suppressLiveParts = false;
        lastRefresh = 0;
        main = null;
        if (target != null) {
            target.destroyBuffers();
            target = null;
        }
        HudWorth.reset();
        MonitorRate.reset();
    }

    // once per frame, before any GUI extraction
    public static void beginFrame(boolean renderLevel) {
        Minecraft mc = Minecraft.getInstance();
        boolean was = active;
        long now = System.nanoTime();
        active = enabled
                && HudWorth.update(now, MonitorRate.intervalNs(now))
                && renderLevel && mc.level != null
                && mc.gui.screen() == null && mc.gui.overlay() == null;
        if (!active) {
            return;
        }
        main = mc.gameRenderer.mainRenderTarget();
        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();
        refresh = !was || target == null || target.width != w || target.height != h
                || now - lastRefresh >= MonitorRate.intervalNs(now);
        if (refresh) {
            lastRefresh = now;
            if (target == null) {
                target = new TextureTarget("lovisual hud", w, h, true, GpuFormat.RGBA8_UNORM);
            } else if (target.width != w || target.height != h) {
                target.resize(w, h);
            }
        }
    }

    public static boolean active() {
        return active;
    }

    public static boolean skipExtraction() {
        return active && !refresh;
    }

    public static RenderTarget drawTarget(RenderTarget requested) {
        return drawing && requested == main ? target : null;
    }

    // depth is cleared to 0: 26.x renders with reversed depth
    public static void drawIntoTexture(Runnable draw) {
        RenderSystem.getDevice().createCommandEncoder()
                .clearColorAndDepthTextures(target.getColorTexture(), CLEAR, target.getDepthTexture(), 0.0);
        drawing = true;
        try {
            draw.run();
        } finally {
            drawing = false;
        }
    }

    public static void composite() {
        RenderTarget main = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(() -> "lovisual hud composite", main.getColorTextureView(), Optional.empty(),
                        main.useDepth ? main.getDepthTextureView() : null, OptionalDouble.empty())) {
            pass.setPipeline(COMPOSITE);
            RenderSystem.bindDefaultUniforms(pass);
            pass.bindTexture("InSampler", target.getColorTextureView(),
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            pass.draw(3, 1, 0, 0);
        }
    }

    private static boolean isModLoaded(String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }
}
