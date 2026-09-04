/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins;

import com.google.common.collect.ImmutableMap;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.shaders.ShaderType;
import net.minecraft.client.renderer.PostChainConfig;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.mixins.accessors.ShaderLoaderDefinitionsAccessor;
import dev.loki.lovisual.mixins.accessors.ShaderSourceKeyAccessor;
import dev.loki.lovisual.render.engine.shader.LoVisualShaderSources;
import dev.loki.lovisual.render.iris.IrisCompatibilityGuards;
import dev.loki.lovisual.util.logging.DebugLog;
import dev.loki.lovisual.util.resources.RenderResourceReadiness;
import dev.loki.lovisual.util.resources.ResourceReloadHooks;

import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.Map;

@Mixin(ShaderManager.class)
public abstract class ShaderLoaderMixin {
    @Unique
    private static final Identifier LOVISUAL_ENTITY_VERTEX = Identifier.fromNamespaceAndPath("lovisual", "shaders/core/entity_dither.vsh");
    @Unique
    private static final Identifier LOVISUAL_ENTITY_FRAGMENT = Identifier.fromNamespaceAndPath("lovisual", "shaders/core/entity_dither.fsh");
    @Unique
    private static final Identifier VANILLA_ENTITY_ID = Identifier.withDefaultNamespace("core/entity");
    @Unique
    private static boolean lovisual$loggedEntityOverride;
    @Unique
    private static boolean lovisual$loggedExtendedShaderSources;
    @Unique
    private static boolean lovisual$loggedShaderLoadError;
    @Unique
    private static Constructor<?> lovisual$shaderSourceKeyConstructor;

    @Unique
    private static int lovisual$shaderLoadFailCount;

    @WrapOperation(
        method = "prepare",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/renderer/ShaderManager;loadShader(Lnet/minecraft/resources/Identifier;Lnet/minecraft/server/packs/resources/Resource;Lcom/mojang/blaze3d/shaders/ShaderType;Ljava/util/Map;Lcom/google/common/collect/ImmutableMap$Builder;)V")
    )
    private static void lovisual$wrapLoadShader(Identifier id, Resource resource, ShaderType type,
                                                  Map<?, ?> allResources, ImmutableMap.Builder<?, ?> builder,
                                                  Operation<Void> original) {
        try {
            original.call(id, resource, type, allResources, builder);
        } catch (Throwable t) {
            lovisual$shaderLoadFailCount++;
            if (!lovisual$loggedShaderLoadError) {
                lovisual$loggedShaderLoadError = true;
                DebugLog.error("[Shader] Failed to load shader", t);
            }
            DebugLog.warn("[Shader] Skipping shader {}: {}", id, t.getMessage());
        }
    }

    @Unique
    private static boolean lovisual$addExtendedShaderSources(ResourceManager resourceManager, Map<Object, String> shaderSources) {
        Map<Identifier, Resource> resources = resourceManager.listResources("shaders", LoVisualShaderSources::isLoVisualExtendedSource);
        if (resources.isEmpty()) {
            return false;
        }

        int added = 0;
        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            Identifier resourceId = entry.getKey();
            ShaderType type = LoVisualShaderSources.typeByLocation(resourceId);
            if (type == null) continue;

            String source = LoVisualShaderSources.load(resourceManager, resourceId, entry.getValue());
            shaderSources.put(lovisual$newShaderSourceKey(resourceId, type), source);
            added++;

            Identifier canonicalId = LoVisualShaderSources.canonicalShaderId(resourceId);
            if (!canonicalId.equals(resourceId)) {
                shaderSources.put(lovisual$newShaderSourceKey(canonicalId, type), source);
                added++;
            }
        }

        if (added > 0 && !lovisual$loggedExtendedShaderSources) {
            lovisual$loggedExtendedShaderSources = true;
            DebugLog.renderThread("[ShaderSource] registered LoVisual .vert/.frag sources in ShaderManager: " + added + " keys");
        }
        return added > 0;
    }

    @Unique
    private static boolean lovisual$allowVanillaEntityShaderOverride() {
        if (IrisCompatibilityGuards.suppressLoVisualTerrainShaderOverrides()) {
            return false;
        }

        return true;
    }

    @Unique
    private static boolean lovisual$applyEntityOverride(ResourceManager resourceManager, Map<Object, String> shaderSources) {
        String entityFragmentSrc = lovisual$loadShaderSource(resourceManager, LOVISUAL_ENTITY_FRAGMENT);
        String entityVertexSrc = lovisual$loadShaderSource(resourceManager, LOVISUAL_ENTITY_VERTEX);
        if (((entityFragmentSrc == null || entityFragmentSrc.isEmpty()) || (entityVertexSrc == null || entityVertexSrc.isEmpty())) && !lovisual$loggedEntityOverride) {
            lovisual$loggedEntityOverride = true;
            DebugLog.warn("[ShaderOverride] entity fade override skipped: lovisual entity_dither shader missing");
        }

        if ((entityFragmentSrc == null || entityFragmentSrc.isEmpty()) || (entityVertexSrc == null || entityVertexSrc.isEmpty())) {
            return false;
        }

        boolean entityFragmentChanged = false;
        boolean entityVertexChanged = false;

        for (Map.Entry<Object, String> entry : shaderSources.entrySet()) {
            Object key = entry.getKey();
            if (entityVertexSrc != null && !entityVertexSrc.isEmpty() && lovisual$isEntityVertexKey(key)) {
                entry.setValue(entityVertexSrc);
                entityVertexChanged = true;
                continue;
            }
            if (entityFragmentSrc != null && !entityFragmentSrc.isEmpty() && lovisual$isEntityFragmentKey(key)) {
                entry.setValue(entityFragmentSrc);
                entityFragmentChanged = true;
            }
        }

        if ((!entityFragmentChanged || !entityVertexChanged) && !lovisual$loggedEntityOverride) {
            lovisual$loggedEntityOverride = true;
            DebugLog.warn("[ShaderOverride] entity fade override skipped: entity shader key not found");
        }

        if (entityFragmentChanged && entityVertexChanged && !lovisual$loggedEntityOverride) {
            lovisual$loggedEntityOverride = true;
            DebugLog.renderThread("[ShaderOverride] override applied: core/entity -> lovisual entity_dither");
        }

        return entityFragmentChanged || entityVertexChanged;
    }

    @Unique
    private static boolean lovisual$isEntityFragmentKey(Object key) {
        if (!(key instanceof ShaderSourceKeyAccessor accessor)) return false;
        return VANILLA_ENTITY_ID.equals(accessor.lovisual$getId()) && accessor.lovisual$getType() == ShaderType.FRAGMENT;
    }

    @Unique
    private static boolean lovisual$isEntityVertexKey(Object key) {
        if (!(key instanceof ShaderSourceKeyAccessor accessor)) return false;
        return VANILLA_ENTITY_ID.equals(accessor.lovisual$getId()) && accessor.lovisual$getType() == ShaderType.VERTEX;
    }

    @Unique
    private static String lovisual$loadShaderSource(ResourceManager resourceManager, Identifier id) {
        try {
            return LoVisualShaderSources.load(resourceManager, id);
        } catch (Exception e) {
            DebugLog.error("[ShaderOverride] read shader override failed: " + id, e);
            return null;
        }
    }

    @Unique
    private static Object lovisual$newShaderSourceKey(Identifier id, ShaderType type) {
        try {
            if (lovisual$shaderSourceKeyConstructor == null) {
                Class<?> keyClass = Class.forName("net.minecraft.client.renderer.ShaderManager$ShaderSourceKey");
                Constructor<?> constructor = keyClass.getDeclaredConstructor(Identifier.class, ShaderType.class);
                constructor.setAccessible(true);
                lovisual$shaderSourceKeyConstructor = constructor;
            }
            return lovisual$shaderSourceKeyConstructor.newInstance(id, type);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to create ShaderManager shader source key for " + type + " " + id, e);
        }
    }

    @Inject(method = "apply", at = @At("HEAD"))
    private void lovisual$beforeShaderReload(ShaderManager.Configs definitions,
                                              ResourceManager resourceManager,
                                              ProfilerFiller profiler,
                                              CallbackInfo ci) {
        RenderResourceReadiness.markReloading("shader reload");
    }

    @Inject(method = "apply", at = @At("TAIL"))
    private void lovisual$afterShaderReload(ShaderManager.Configs definitions,
                                             ResourceManager resourceManager,
                                             ProfilerFiller profiler,
                                             CallbackInfo ci) {
        ResourceReloadHooks.onReload(resourceManager);
    }

    @ModifyReturnValue(method = "prepare", at = @At("RETURN"))
    private ShaderManager.Configs lovisual$extendShaderSources(ShaderManager.Configs definitions,
                                                                ResourceManager resourceManager,
                                                                ProfilerFiller profiler) {
        ShaderLoaderDefinitionsAccessor accessor = (ShaderLoaderDefinitionsAccessor) (Object) definitions;
        Map<?, String> shaderSources = accessor.lovisual$getShaderSources();
        Map<Object, String> replaced = new LinkedHashMap<>(shaderSources.size() + 64);
        replaced.putAll(shaderSources);

        boolean changed = lovisual$addExtendedShaderSources(resourceManager, replaced);
        if (lovisual$allowVanillaEntityShaderOverride()) {
            changed |= lovisual$applyEntityOverride(resourceManager, replaced);
        }

        if (!changed) {
            return definitions;
        }

        Map<Identifier, PostChainConfig> postChains = accessor.lovisual$getPostChains();
        @SuppressWarnings({"rawtypes", "unchecked"})
        ShaderManager.Configs updated = new ShaderManager.Configs((Map) replaced, postChains);
        return updated;
    }
}
