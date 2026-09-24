package dev.loki.lovisual.render.engine.visuals;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Base contract for the custom world visual graph.
 */
public interface LoVisualVisualPass {
    Identifier getId();

    LoVisualVisualPhase getPhase();

    default boolean isEnabled() {
        return true;
    }

    default void init() {
    }

    default void onResourceReload(ResourceManager manager) {
    }

    default void prepareFrame(LoVisualVisualFrame frame) {
    }

    default boolean render(LoVisualVisualFrame frame, GpuTextureView src, GpuTextureView dst) {
        return false;
    }
}
