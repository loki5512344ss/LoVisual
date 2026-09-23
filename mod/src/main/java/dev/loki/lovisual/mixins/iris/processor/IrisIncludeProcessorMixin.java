/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.iris.processor;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.shaderpack.include.AbsolutePackPath;
import net.irisshaders.iris.shaderpack.include.IncludeProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import dev.loki.lovisual.render.iris.patch.ShaderPatchEngine;
import dev.loki.lovisual.util.logging.DebugLog;

import java.util.Objects;

@Pseudo
@Mixin(value = IncludeProcessor.class, remap = false)
public abstract class IrisIncludeProcessorMixin {
    @Unique
    private ShaderPatchEngine.Session lovisual$patchSession;
    @Unique
    private String lovisual$patchSessionPackName;

    @Unique
    private static String lovisual$currentShaderPackName() {
        String loadingPackName = ShaderPatchEngine.loadingShaderPackName();
        if (loadingPackName != null && !loadingPackName.isBlank()) {
            return loadingPackName;
        }
        try {
            return Iris.getCurrentPackName();
        } catch (Throwable throwable) {
            DebugLog.warn("[IrisPatch] Unable to query current Iris shaderpack name: %s", throwable.getClass().getSimpleName());
            return null;
        }
    }

    @ModifyReturnValue(method = "getIncludedFile", at = @At("RETURN"), remap = false)
    private ImmutableList<String> lovisual$compileShaderpackPatch(ImmutableList<String> lines, AbsolutePackPath path) {
        if (lines == null || path == null) {
            return lines;
        }

        String packName = lovisual$currentShaderPackName();
        if (lovisual$patchSession == null || !Objects.equals(packName, lovisual$patchSessionPackName)) {
            lovisual$patchSessionPackName = packName;
            lovisual$patchSession = ShaderPatchEngine.newSession(packName);
            DebugLog.info("[IrisPatch] IncludeProcessor hook active: shaderPack='%s' active=%s firstPath=%s",
                    packName == null ? "" : packName,
                    lovisual$patchSession.isActive(),
                    path.getPathString());
        }

        if (!lovisual$patchSession.isActive()) {
            return lines;
        }

        IncludeProcessor processor = (IncludeProcessor) (Object) this;
        return lovisual$patchSession.patch(
                path.getPathString(),
                lines,
                candidate -> processor.getIncludedFile(AbsolutePackPath.fromAbsolutePath(candidate))
        );
    }
}
