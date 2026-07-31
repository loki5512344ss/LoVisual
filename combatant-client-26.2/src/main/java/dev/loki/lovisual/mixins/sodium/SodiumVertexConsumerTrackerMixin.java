/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.sodium;

import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import dev.loki.lovisual.util.logging.DebugLog;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.vertex.VertexConsumerTracker", remap = false)
public abstract class SodiumVertexConsumerTrackerMixin {
    @Redirect(
            method = "logBadConsumer",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;)V",
                    remap = false
            ),
            remap = false
    )
    private static void lovisual$redirectBadConsumerWarn(Logger logger, String message, Object arg) {
        DebugLog.warn("[Sodium] %s", lovisual$formatSlf4jSingleArg(message, arg));
    }

    @Unique
    private static String lovisual$formatSlf4jSingleArg(String message, Object arg) {
        String safeMessage = String.valueOf(message);
        int placeholder = safeMessage.indexOf("{}");
        if (placeholder < 0) {
            return safeMessage + " " + arg;
        }
        return safeMessage.substring(0, placeholder)
                + arg
                + safeMessage.substring(placeholder + 2);
    }
}
