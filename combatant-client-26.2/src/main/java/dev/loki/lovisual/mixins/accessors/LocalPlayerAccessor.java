/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.accessors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LocalPlayer.class)
public interface LocalPlayerAccessor {

    @Accessor("minecraft")
    Minecraft getClient();

    @Accessor("portalEffectIntensity")
    float lovisual$getNauseaIntensity();

    @Accessor("portalEffectIntensity")
    void lovisual$setNauseaIntensity(float value);

    @Accessor("oPortalEffectIntensity")
    float lovisual$getLastNauseaIntensity();

    @Accessor("oPortalEffectIntensity")
    void lovisual$setLastNauseaIntensity(float value);

    @Accessor("positionReminder")
    void lovisual$setTicksSinceLastPositionPacketSent(int ticks);

    @Accessor("wasSprinting")
    void lovisual$setLastSprinting(boolean sprinting);

    @Invoker("canStartSprinting")
    boolean lovisual$canStartSprinting();

    @Invoker("shouldStopRunSprinting")
    boolean lovisual$shouldStopSprinting();

    @Invoker("shouldStopSwimSprinting")
    boolean lovisual$shouldStopSwimSprinting();
}

