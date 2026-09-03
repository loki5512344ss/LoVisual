/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins;

import com.mojang.blaze3d.systems.GpuSurface;
import dev.loki.lovisual.features.module.modules.combat.*;
import net.minecraft.client.FramerateLimiter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.loki.lovisual.events.Events;
import dev.loki.lovisual.events.impl.CrosshairTargetUpdateEvent;
import dev.loki.lovisual.features.gui.chat.BetterChatStoreManager;
import dev.loki.lovisual.features.module.ModuleManager;
import dev.loki.lovisual.features.module.Modules;
import dev.loki.lovisual.features.module.modules.player.NoDelay;
import dev.loki.lovisual.features.module.modules.player.NoInteract;
import dev.loki.lovisual.features.module.modules.visuals.Freecam;
import dev.loki.lovisual.features.relations.StaffTracker;
import dev.loki.lovisual.render.helpers.TickDelta;
import dev.loki.lovisual.util.combat.AntiBotTracker;
import dev.loki.lovisual.util.session.SessionChanger;
import dev.loki.lovisual.util.session.MinecraftGameConfigHolder;

@Mixin(Minecraft.class)
public class MinecraftMixin implements MinecraftGameConfigHolder {

    @Shadow
    @Final
    private GpuSurface windowSurface;

    @Unique
    private GameConfig lovisual$gameConfig;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void lovisual$captureGameConfig(GameConfig config, CallbackInfo ci) {
        this.lovisual$gameConfig = config;
    }

    @Override
    public GameConfig lovisual$getGameConfig() {
        return lovisual$gameConfig;
    }

    @Unique
    private static BlockHitResult lovisual$miss(Vec3 pos) {
        return new BlockHitResult(
                pos,
                Direction.UP,
                BlockPos.ZERO,
                false
        );
    }

    @Unique
    private static void lovisual$guardSingleplayerSaveWithAltUsername() {
        SessionChanger.restoreSingleplayerJoinSessionForDisconnect();
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void fastThrow(CallbackInfo ci) {
        NoDelay noDelay = Modules.get(NoDelay.class);
        if (noDelay != null && noDelay.handleFastUse()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "handleKeybinds",
            at = @At("HEAD")
    )
    private void spearassist$blockAttackInput(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        SpearAssist assist = Modules.get(SpearAssist.class);
        if (assist == null || !assist.isEnabled() || !assist.respectCooldownEnabled()) {
            return;
        }

        ItemStack stack = mc.player.getMainHandItem();
        if (!SpearAssist.isSpear(stack)) return;

        float cooldown = mc.player.getAttackStrengthScale(0.0F);
        if (cooldown < 0.99F) {
            // ВАЖНО: гасим сам ввод
            mc.options.keyAttack.setDown(false);
        }
    }

    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void lovisual$disablePvpGlow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof Player)) return;
        PvpCooldowns mod = Modules.get(PvpCooldowns.class);
        if (mod == null || !mod.shouldHideTargetGlow()) return;
        cir.setReturnValue(false);
    }


    // === КУРСОР ДЛЯ CLICKGUI ===

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void onHandleBlockBreaking(boolean breaking, CallbackInfo ci) {
        // AutoAttack / Hitbox modules removed; no block-breaking override remains.
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void onDoAttack(CallbackInfoReturnable<Boolean> cir) {
        // AttributeSwap / AutoAttack modules removed; no attack override remains.
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", at = @At("HEAD"))
    private void lovisual$betterChat$saveOnDisconnect(net.minecraft.client.gui.screens.Screen screen, boolean transferring, boolean bl, CallbackInfo ci) {
        lovisual$guardSingleplayerSaveWithAltUsername();
        try {
            BetterChatStoreManager.flushAll();
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", at = @At("TAIL"))
    private void lovisual$sessionRestoreAfterDisconnect(net.minecraft.client.gui.screens.Screen screen, boolean transferring, boolean bl, CallbackInfo ci) {
        SessionChanger.restoreDeferredSessionAfterDisconnect();
    }

    @Inject(method = "pick", at = @At("HEAD"), cancellable = true)
    private void onUpdateCrosshairTarget(float tickDelta, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        var fc = Modules.get(Freecam.class);

        // =========================================================
        // FREECAM
        // =========================================================
        if (fc != null && fc.isEnabled()) {

            // позиция и направление FREECAM
            Vec3 start = new Vec3(
                    Mth.lerp(tickDelta, fc.camPosPrev.x, fc.camPos.x),
                    Mth.lerp(tickDelta, fc.camPosPrev.y, fc.camPos.y),
                    Mth.lerp(tickDelta, fc.camPosPrev.z, fc.camPos.z)
            );

            float yawRad = -fc.camYaw * ((float) Math.PI / 180F);
            float pitchRad = -fc.camPitch * ((float) Math.PI / 180F);

            Vec3 dir = new Vec3(
                    Mth.sin(yawRad) * Mth.cos(pitchRad),
                    Mth.sin(pitchRad),
                    Mth.cos(yawRad) * Mth.cos(pitchRad)
            );

            // ===============================
            // FREECAM + NO INTERACT
            // ===============================
            if (!fc.allowInteract()) {
                mc.hitResult = lovisual$miss(start);
                mc.crosshairPickEntity = null;
                ci.cancel();
                return;
            }

            // ===============================
            // FREECAM + INTERACT
            // ===============================

            // Blocks (FREECAM)
            double reach = mc.player.blockInteractionRange();
            Vec3 end = start.add(dir.scale(reach));

            BlockHitResult bhr = mc.level.clip(new net.minecraft.world.level.ClipContext(
                    start, end,
                    net.minecraft.world.level.ClipContext.Block.OUTLINE,
                    net.minecraft.world.level.ClipContext.Fluid.NONE,
                    mc.player
            ));

            mc.hitResult = bhr;
            mc.crosshairPickEntity = null;
            ci.cancel();
            return;
        }
    }

    @Inject(method = "pick", at = @At("RETURN"))
    private void lovisual$dispatchCrosshairTargetEvent(float tickDelta, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        CrosshairTargetUpdateEvent event = new CrosshairTargetUpdateEvent(
                tickDelta,
                mc.hitResult,
                mc.crosshairPickEntity
        );
        Events.BUS.post(event);
        mc.hitResult = event.getHitResult();
        mc.crosshairPickEntity = event.getTargetedEntity();
    }

    @Inject(method = "pick", at = @At("RETURN"))
    private void noInteract$afterRaycast(float tickDelta, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        NoInteract noInteract = Modules.get(NoInteract.class);
        if (noInteract == null || !noInteract.shouldBlockBlockInteraction()) return;

        var hit = mc.hitResult;
        if (hit instanceof EntityHitResult) return;

        Vec3 camPos = mc.player.getEyePosition(tickDelta);
        mc.hitResult = new BlockHitResult(
                camPos,
                Direction.UP,
                BlockPos.ZERO,
                false
        );
        mc.crosshairPickEntity = null;
    }

    @Inject(method = "disconnect", at = @At("HEAD"))
    private void freecam$onDisconnect(CallbackInfo ci) {
        Freecam fc = Modules.get(Freecam.class);
        if (fc != null && fc.isEnabled()) {
            fc.toggle();
        }
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void freecam$onJoinWorld(ClientLevel world, CallbackInfo ci) {
        Freecam fc = Modules.get(Freecam.class);
        if (fc != null && fc.isEnabled()) {
            fc.toggle();
        }
    }

    @Inject(method = "setLevel", at = @At("TAIL"))
    private void lovisual$captureSingleplayerJoinSession(ClientLevel world, CallbackInfo ci) {
        if (world == null) {
            SessionChanger.clearSingleplayerJoinSession();
        } else {
            SessionChanger.captureSingleplayerJoinSession();
        }
    }

    @Inject(method = "clearDownloadedResourcePacks", at = @At("HEAD"))
    private void lovisual$onDisconnected(CallbackInfo ci) {
        StaffTracker.INSTANCE.resetAll();
        AntiBotTracker.INSTANCE.reset();
    }

    @Inject(
            method = "renderFrame",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/DeltaTracker$Timer;advanceRealTime(J)V",
                    shift = At.Shift.AFTER
            )
    )
    private void lovisual$frameModulesAfterRenderTick(boolean tick, CallbackInfo ci) {
        ModuleManager.frameAll(TickDelta.frameDeltaTicks());
    }

    @Inject(
            method = "renderFrame",
            at = @At(
                    value = "INVOKE_STRING",
                    target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
                    args = "ldc=frameLimiter",
                    shift = At.Shift.AFTER
            )
    )
    private void lovisual$paceImmediateNoVsync(boolean tick, CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        if (mc == null || mc.options == null || mc.getWindow() == null || windowSurface == null) {
            return;
        }
        if (Boolean.TRUE.equals(mc.options.enableVsync().get())) {
            return;
        }
        if (windowSurface.currentConfiguration().isEmpty()
                || windowSurface.currentConfiguration().get().presentMode() != GpuSurface.PresentMode.IMMEDIATE) {
            return;
        }

        int configuredLimit = mc.gameRenderer.gameRenderState().framerateLimit;
        if (configuredLimit < 260) {
            return;
        }

        int refreshRate = mc.getWindow().getRefreshRate();
        int targetFps = lovisual$immediatePacingTarget(refreshRate);
        FramerateLimiter.limitDisplayFPS(targetFps);
    }

    @Unique
    private static int lovisual$immediatePacingTarget(int refreshRate) {
        int base = refreshRate > 0 ? refreshRate : 120;
        return Mth.clamp(base * 2, 120, 360);
    }

    @Inject(method = "getTickTargetMillis", at = @At("RETURN"), cancellable = true)
    private void lovisual$applyTimer(float millis, CallbackInfoReturnable<Float> cir) {
        // Timer module removed; tick speed remains unchanged.
    }

    @Inject(method = "isLevelRunningNormally", at = @At("HEAD"), cancellable = true)
    private void lovisual$skipPlayerDependentTicksWithoutPlayer(CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player == null) {
            cir.setReturnValue(false);
        }
    }
}
