/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.hud.bars;

import dev.loki.lovisual.events.Events;
import dev.loki.lovisual.events.impl.pvp.CombatProtocolBossbarEvent;
import dev.loki.lovisual.mixins.accessors.hud.BossHealthOverlayAccessor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import dev.loki.lovisual.util.pvp.client.CooldownsState;

@Mixin(BossHealthOverlay.class)
public class BossHealthOverlayMixin {

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void onRender(GuiGraphicsExtractor ctx, CallbackInfo ci) {
        if (Events.BUS.hasListeners(CombatProtocolBossbarEvent.class)) {
            List<String> names = new ArrayList<>();
            for (LerpingBossEvent bar : ((BossHealthOverlayAccessor) this).getBars().values()) {
                if (bar == null) continue;
                Component name = bar.getName();
                if (name != null && !name.getString().isBlank()) {
                    names.add(name.getString());
                }
            }
            if (!names.isEmpty()) {
                Events.BUS.post(new CombatProtocolBossbarEvent(names));
            }
        }

        // PvP bossbar detection disabled: bossbar no longer reliable.
        /*
        BossBarHud hud = (BossBarHud) (Object) this;

        Map<UUID, ClientBossBar> bars =
                ((BossHealthOverlayAccessor) hud).getBars();

        boolean pvpActive = false;

        // проверяем только боссбары
        for (ClientBossBar bar : bars.values()) {

            String name = bar.getName().getString();

            if (name.startsWith("Режим PVP -")) {
                pvpActive = true;
                break;
            }
        }

        // === только тут меняем состояние PvP ===

        if (pvpActive && !CooldownsState.MANAGER.isInPvp()) {
            //System.out.println("[PvPDetector] → ENTER PVP");
            CooldownsState.MANAGER.enterPvp();
        }

        if (!pvpActive && CooldownsState.MANAGER.isInPvp()) {
            //System.out.println("[PvPDetector] → EXIT PVP");
            CooldownsState.MANAGER.exitPvp();
        }
        */
    }
}

