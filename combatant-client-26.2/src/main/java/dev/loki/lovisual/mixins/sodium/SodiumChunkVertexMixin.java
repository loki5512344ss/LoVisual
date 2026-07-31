/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.sodium;

import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.render.sodium.terrain.LoVisualChunkVertexExtension;

@Mixin(ChunkVertexEncoder.Vertex.class)
public abstract class SodiumChunkVertexMixin implements LoVisualChunkVertexExtension {
    @Unique
    private int lovisual$surfaceFlags;

    @Inject(method = "copyVertexTo", at = @At("HEAD"))
    private static void lovisual$copyVertexData(ChunkVertexEncoder.Vertex from, ChunkVertexEncoder.Vertex to, CallbackInfo ci) {
        ((LoVisualChunkVertexExtension) from).lovisual$copyData((LoVisualChunkVertexExtension) to);
    }

    @Override
    public void lovisual$setSurfaceFlags(int surfaceFlags) {
        this.lovisual$surfaceFlags = surfaceFlags;
    }

    @Override
    public int lovisual$getSurfaceFlags() {
        return this.lovisual$surfaceFlags;
    }

    @Override
    public void lovisual$copyData(LoVisualChunkVertexExtension dest) {
        dest.lovisual$setSurfaceFlags(this.lovisual$surfaceFlags);
    }
}
