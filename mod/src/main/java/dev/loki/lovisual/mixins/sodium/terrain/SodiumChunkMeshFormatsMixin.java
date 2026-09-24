package dev.loki.lovisual.mixins.sodium.terrain;

import dev.loki.lovisual.render.sodium.terrain.LoVisualChunkMeshFormats;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkMeshFormats;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sodium 0.9.1 routes the vertex format used by the renderer, builder and
 * region arenas through this method. Replacing it here keeps all three users
 * on LoVisual's extended 24-byte layout.
 */
@Mixin(ChunkMeshFormats.class)
public abstract class SodiumChunkMeshFormatsMixin {
    @Inject(method = "getCurrent", at = @At("HEAD"), cancellable = true, remap = false)
    private static void lovisual$useExtendedVertexFormat(CallbackInfoReturnable<ChunkVertexType> cir) {
        cir.setReturnValue(LoVisualChunkMeshFormats.SURFACE_FLAGS);
    }
}
