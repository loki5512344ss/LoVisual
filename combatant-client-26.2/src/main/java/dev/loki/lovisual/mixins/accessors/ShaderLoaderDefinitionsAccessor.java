/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.accessors;

import net.minecraft.client.renderer.PostChainConfig;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ShaderManager.Configs.class)
public interface ShaderLoaderDefinitionsAccessor {
    @Accessor("shaderSources")
    Map<?, String> lovisual$getShaderSources();

    @Accessor("postChains")
    Map<Identifier, PostChainConfig> lovisual$getPostChains();
}
